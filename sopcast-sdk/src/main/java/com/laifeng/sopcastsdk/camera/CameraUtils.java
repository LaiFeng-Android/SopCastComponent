package com.laifeng.sopcastsdk.camera;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;

import com.laifeng.sopcastsdk.configuration.CameraConfiguration;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.blacklist.BlackListHelper;
import com.laifeng.sopcastsdk.camera.exception.CameraDisabledException;
import com.laifeng.sopcastsdk.camera.exception.CameraNotSupportException;
import com.laifeng.sopcastsdk.camera.exception.NoCameraException;
import com.laifeng.sopcastsdk.utils.SopCastLog;

import java.util.ArrayList;
import java.util.List;


/**
 * @Title: CameraUtils
 * @Package com.youku.crazytogether.app.modules.sopCastV2
 * @Description:
 * @Author Jim
 * @Date 16/3/23
 * @Time 下午12:01
 * @Version
 */
@TargetApi(14)
public class CameraUtils {

    private static final int CAPTURE_WIDTH_MIN_V1 = 1280;
    private static final int CAPTURE_HEIGHT_MIN_V1 = 720;
    private static final int CAPTURE_WIDTH_MIN_V2 = 640;
    private static final int CAPTURE_HEIGHT_MIN_V2 = 360;

    public static List<CameraData> getAllCamerasData(boolean isBackFirst) {
        ArrayList<CameraData> cameraDatas = new ArrayList<>();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                CameraData cameraData = new CameraData(i, CameraData.FACING_FRONT);
                if(isBackFirst) {
                    cameraDatas.add(cameraData);
                } else {
                    cameraDatas.add(0, cameraData);
                }
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                CameraData cameraData = new CameraData(i, CameraData.FACING_BACK);
                if(isBackFirst) {
                    cameraDatas.add(0, cameraData);
                } else {
                    cameraDatas.add(cameraData);
                }
            }
        }
        return cameraDatas;
    }

    public static void initCameraParams(Camera camera, CameraData cameraData, boolean isTouchMode, CameraConfiguration configuration)
            throws CameraNotSupportException {
        boolean isLandscape = (configuration.orientation != CameraConfiguration.Orientation.PORTRAIT);
        int cameraWidth = Math.max(configuration.height, configuration.width);
        int cameraHeight = Math.min(configuration.height, configuration.width);
        Camera.Parameters parameters = camera.getParameters();
        setPreviewFormat(camera, parameters);
        setPreviewFps(camera, configuration.fps, parameters);
        setPreviewSize(camera, cameraData, cameraWidth, cameraHeight, parameters);
        cameraData.hasLight = supportFlash(camera);
        setOrientation(cameraData, isLandscape);
        setFocusMode(camera, cameraData, isTouchMode);
    }

    public static void setPreviewFormat(Camera camera, Camera.Parameters parameters) throws CameraNotSupportException{
        //设置预览回调的图片格式
        try {
            parameters.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(parameters);
        } catch (Exception e) {
            throw new CameraNotSupportException();
        }
    }

    public static void setPreviewFps(Camera camera, int fps, Camera.Parameters parameters) {
        //设置摄像头预览帧率
        if(BlackListHelper.deviceInFpsBlacklisted()) {
            SopCastLog.d(SopCastConstant.TAG, "Device in fps setting black list, so set the camera fps 15");
            fps = 15;
        }
        try {
            parameters.setPreviewFrameRate(fps);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();
        for (int[] range:supportedPreviewFpsRange) {
            if(range.length == 2 && range[0] == fps*1000 && range[1] == fps*1000) {
                //设置预览帧数
                try {
                    parameters.setPreviewFpsRange(fps*1000, fps*1000);
                    camera.setParameters(parameters);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public static void setPreviewSize(Camera camera, CameraData cameraData, int width, int height,
                                      Camera.Parameters parameters) throws CameraNotSupportException {
        Camera.Size size = getOptimalPreviewSize(camera, width, height);
        if(size == null) {
            throw new CameraNotSupportException();
        }else {
            cameraData.cameraWidth = size.width;
            cameraData.cameraHeight = size.height;
        }
        //设置预览大小
        SopCastLog.d(SopCastConstant.TAG, "Camera Width: " + size.width + "    Height: " + size.height);
        try {
            parameters.setPreviewSize(cameraData.cameraWidth, cameraData.cameraHeight);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setOrientation(CameraData cameraData, boolean isLandscape) {
        int orientation = getDisplayOrientation(cameraData.cameraID);
        if(isLandscape) {
            cameraData.orientation = orientation - 90;
        } else {
            cameraData.orientation = orientation;
        }
    }

    private static void setFocusMode(Camera camera, CameraData cameraData, boolean isTouchMode) {
        cameraData.supportTouchFocus = supportTouchFocus(camera);
        if(!cameraData.supportTouchFocus) {
            setAutoFocusMode(camera);
        } else {
            if(!isTouchMode) {
                cameraData.touchFocusMode = false;
                setAutoFocusMode(camera);
            }else {
                cameraData.touchFocusMode = true;
            }
        }
    }

    public static boolean supportTouchFocus(Camera camera) {
        if(camera != null) {
            return (camera.getParameters().getMaxNumFocusAreas() != 0);
        }
        return false;
    }

    public static void setAutoFocusMode(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.size() > 0 && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parameters);
            } else if (focusModes.size() > 0) {
                parameters.setFocusMode(focusModes.get(0));
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setTouchFocusMode(Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.size() > 0 && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            } else if (focusModes.size() > 0) {
                parameters.setFocusMode(focusModes.get(0));
                camera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Camera.Size getOptimalPreviewSize(Camera camera, int width, int height) {
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null) return null;
        //寻找和给出的宽高相等的参数
        for(Camera.Size size:sizes){
            if(size.width == width && size.height == height) {
                optimalSize = size;
            }
        }
        //寻找最接近且大于给定宽高的
        if(optimalSize == null) {
            for(Camera.Size size:sizes){
                if(size.width >= width && size.height >= height ) {
                    if (Math.abs(size.height - height) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - height);
                    }
                }
            }
        }
        //选择一个宽高大于1280*720且高度最接近720的
        if(optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for(Camera.Size size:sizes){
                if(size.width >= CAPTURE_WIDTH_MIN_V1 && size.height >= CAPTURE_HEIGHT_MIN_V1) {
                    if (Math.abs(size.height - height) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - height);
                    }
                }
            }
        }
        //如果上面条件都不满足，那么选择一个宽高大于640*360且高度最接近360的
        if(optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for(Camera.Size size:sizes){
                if(size.width >= CAPTURE_WIDTH_MIN_V2 && size.height >= CAPTURE_HEIGHT_MIN_V2) {
                    if (Math.abs(size.height - height) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - height);
                    }
                }
            }
        }
        return optimalSize;
    }

    public static int getDisplayOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation + 360) % 360;
        }
        return result;
    }

    public static void checkCameraService(Context context)
            throws CameraDisabledException, NoCameraException {
        // Check if device policy has disabled the camera.
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm.getCameraDisabled(null)) {
            throw new CameraDisabledException();
        }
        List<CameraData> cameraDatas = getAllCamerasData(false);
        if(cameraDatas.size() == 0) {
            throw new NoCameraException();
        }
    }

    public static boolean supportFlash(Camera camera){
        Camera.Parameters params = camera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        if(flashModes == null) {
            return false;
        }
        for(String flashMode : flashModes) {
            if(Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                return true;
            }
        }
        return false;
    }
}
