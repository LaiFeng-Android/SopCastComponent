/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yzq.zxinglibrary.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.yzq.zxinglibrary.R;
import com.yzq.zxinglibrary.android.CaptureActivityHandler;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one
 * talking to it. The implementation encapsulates the steps needed to take
 * preview-sized images, which are used for both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private static CameraManager cameraManager;

    private final Context context;
    private final CameraConfigurationManager configManager;
    private ZxingConfig config;
    private Camera camera;
    private AutoFocusManager autoFocusManager;
    private Rect framingRect;
    private Rect framingRectInPreview;
    private boolean initialized;
    private boolean previewing;
    private int requestedCameraId = -1;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;
    /**
     * Preview frames are delivered here, which we pass on to the registered
     * handler. Make sure to clear the handler so it will only receive one
     * message.
     */
    private final PreviewCallback previewCallback;

    public CameraManager(Context context, ZxingConfig config) {
        this.context = context;
        this.configManager = new CameraConfigurationManager(context);
        previewCallback = new PreviewCallback(configManager);
        this.config = config;
    }

//    public static void init(Context context) {
//        if (cameraManager == null) {
//            cameraManager = new CameraManager(context, null);
//        }
//    }


    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames
     *               into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public synchronized void openDriver(SurfaceHolder holder)
            throws IOException {
        Camera theCamera = camera;

        if (theCamera == null) {

            if (requestedCameraId >= 0) {
                theCamera = OpenCameraInterface.open(requestedCameraId);
            } else {
                theCamera = OpenCameraInterface.open();
            }

            if (theCamera == null) {
                throw new IOException();
            }
            camera = theCamera;
        }
        theCamera.setPreviewDisplay(holder);

        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(theCamera);
            if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
                setManualFramingRect(requestedFramingRectWidth,
                        requestedFramingRectHeight);
                requestedFramingRectWidth = 0;
                requestedFramingRectHeight = 0;
            }
        }

        Camera.Parameters parameters = theCamera.getParameters();
        String parametersFlattened = parameters == null ? null : parameters
                .flatten(); // Save these, temporarily
        try {
            configManager.setDesiredCameraParameters(theCamera);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG,
                    "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: "
                    + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = theCamera.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    theCamera.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG,
                            "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }

    }

    public synchronized boolean isOpen() {
        return camera != null;
    }

    /**
     * Closes the camera driver if still in use.
     */
    public synchronized void closeDriver() {
        if (camera != null) {
            camera.release();
            camera = null;
            // Make sure to clear these each time we close the camera, so that
            // any scanning rect
            // requested by intent is forgotten.
            framingRect = null;
            framingRectInPreview = null;
        }
    }


    /*切换闪光灯*/
    public void switchFlashLight(CaptureActivityHandler handler) {
        //  Log.i("打开闪光灯", "openFlashLight");

        Camera.Parameters parameters = camera.getParameters();

        Message msg = new Message();

        String flashMode = parameters.getFlashMode();

        if (flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
            /*关闭闪光灯*/
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

            msg.what = Constant.FLASH_CLOSE;


        } else {
            /*打开闪光灯*/
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            msg.what = Constant.FLASH_OPEN;
        }
        camera.setParameters(parameters);
        handler.sendMessage(msg);
    }


    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public synchronized void startPreview() {
        Camera theCamera = camera;
        if (theCamera != null && !previewing) {
            theCamera.startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(camera);
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public synchronized void stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null && previewing) {
            camera.stopPreview();
            previewCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data
     * will arrive as byte[] in the message.obj field, with width and height
     * encoded as message.arg1 and message.arg2, respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public synchronized void requestPreviewFrame(Handler handler, int message) {
        Camera theCamera = camera;
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message);
            theCamera.setOneShotPreviewCallback(previewCallback);
        }
    }


    /*取景框*/
    public synchronized Rect getFramingRect() {
        if (framingRect == null) {
            if (camera == null) {
                return null;
            }
            Point screenResolution = configManager.getScreenResolution();
            if (screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

            int screenResolutionX = screenResolution.x;

            int width = (int) (screenResolutionX * 0.6);
            int height = width;


            /*水平居中  偏上显示*/
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 5;

            framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
                    topOffset + height);
            Log.d(TAG, "Calculated framing rect: " + framingRect);
        }
        return framingRect;
    }


    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview
     * frame, not UI / screen.
     *
     * @return {@link Rect} expressing barcode scan area in terms of the preview
     * size
     */
    public synchronized Rect getFramingRectInPreview() {
        if (framingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Point cameraResolution = configManager.getCameraResolution();
            Point screenResolution = configManager.getScreenResolution();
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

            /******************** 竖屏更改1(cameraResolution.x/y互换) ************************/
            rect.left = rect.left * cameraResolution.y / screenResolution.x;
            rect.right = rect.right * cameraResolution.y / screenResolution.x;
            rect.top = rect.top * cameraResolution.x / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
            framingRectInPreview = rect;
        }
        return framingRectInPreview;
    }

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means
     *                 "no preference".
     */
    public synchronized void setManualCameraId(int cameraId) {
        requestedCameraId = cameraId;
    }

    /**
     * Allows third party apps to specify the scanning rectangle dimensions,
     * rather than determine them automatically based on screen resolution.
     *
     * @param width  The width in pixels to scan.
     * @param height The height in pixels to scan.
     */
    public synchronized void setManualFramingRect(int width, int height) {
        if (initialized) {
            Point screenResolution = configManager.getScreenResolution();
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 5;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
                    topOffset + height);
            Log.d(TAG, "Calculated manual framing rect: " + framingRect);
            framingRectInPreview = null;
        } else {
            requestedFramingRectWidth = width;
            requestedFramingRectHeight = height;
        }
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on
     * the format of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
                                                         int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.


        if (config == null) {
            config = new ZxingConfig();
        }

        if (config.isFullScreenScan()) {
            return new PlanarYUVLuminanceSource(data, width, height, 0,
                    0, width, height, false);
        } else {
            int actionbarHeight = context.getResources().getDimensionPixelSize(R.dimen.toolBarHeight);
            return new PlanarYUVLuminanceSource(data, width, height, rect.left,
                    rect.top + actionbarHeight, rect.width(), rect.height(), false);
        }


    }

    public static CameraManager get() {
        return cameraManager;
    }
}
