package com.laifeng.sopcastsdk.video;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.laifeng.sopcastsdk.camera.CameraData;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.entity.Watermark;
import com.laifeng.sopcastsdk.entity.WatermarkPosition;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @Title: RenderSrfTex
 * @Package com.laifeng.sopcastsdk.video
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:17
 * @Version
 */
@TargetApi(18)
public class RenderSrfTex {
    private final FloatBuffer mNormalVtxBuf = GlUtil.createVertexBuffer();
    private final FloatBuffer mNormalTexCoordBuf = GlUtil.createTexCoordBuffer();

    private int mFboTexId;
    private final MyRecorder mRecorder;

    private final float[] mSymmetryMtx = GlUtil.createIdentityMtx();
    private final float[] mNormalMtx = GlUtil.createIdentityMtx();

    private int mProgram         = -1;
    private int maPositionHandle = -1;
    private int maTexCoordHandle = -1;
    private int muSamplerHandle  = -1;
    private int muPosMtxHandle   = -1;

    private EGLDisplay mSavedEglDisplay     = null;
    private EGLSurface mSavedEglDrawSurface = null;
    private EGLSurface mSavedEglReadSurface = null;
    private EGLContext mSavedEglContext     = null;

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    private FloatBuffer mCameraTexCoordBuffer;

    private Bitmap mWatermarkImg;
    private FloatBuffer mWatermarkVertexBuffer;
    private int mWatermarkTextureId = -1;

    public RenderSrfTex(int id, MyRecorder recorder) {
        mFboTexId = id;
        mRecorder = recorder;
    }

    public void setTextureId(int textureId) {
        mFboTexId = textureId;
    }

    public void setWatermark(Watermark watermark) {
        mWatermarkImg = watermark.markImg;
        initWatermarkVertexBuffer(watermark.width, watermark.height, watermark.orientation, watermark.vMargin, watermark.hMargin);
    }

    private void initWatermarkVertexBuffer(int width, int height, int orientation, int vMargin, int hMargin) {

        boolean isTop, isRight;
        if(orientation == WatermarkPosition.WATERMARK_ORIENTATION_TOP_LEFT
                || orientation == WatermarkPosition.WATERMARK_ORIENTATION_TOP_RIGHT) {
            isTop = true;
        } else {
            isTop = false;
        }

        if(orientation == WatermarkPosition.WATERMARK_ORIENTATION_TOP_RIGHT
                || orientation == WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT) {
            isRight = true;
        } else {
            isRight = false;
        }

        float leftX = (mVideoWidth/2.0f - hMargin - width)/(mVideoWidth/2.0f);
        float rightX = (mVideoWidth/2.0f - hMargin)/(mVideoWidth/2.0f);

        float topY = (mVideoHeight/2.0f - vMargin)/(mVideoHeight/2.0f);
        float bottomY = (mVideoHeight/2.0f - vMargin - height)/(mVideoHeight/2.0f);

        float temp;

        if(!isRight) {
            temp = leftX;
            leftX = -rightX;
            rightX = -temp;
        }
        if(!isTop) {
            temp = topY;
            topY = -bottomY;
            bottomY = -temp;
        }
        final float watermarkCoords[]= {
                leftX,  bottomY, 0.0f,
                leftX, topY, 0.0f,
                rightX,  bottomY, 0.0f,
                rightX, topY, 0.0f
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(watermarkCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mWatermarkVertexBuffer = bb.asFloatBuffer();
        mWatermarkVertexBuffer.put(watermarkCoords);
        mWatermarkVertexBuffer.position(0);
    }

    public void setVideoSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        initCameraTexCoordBuffer();
    }

    private void initCameraTexCoordBuffer() {
        int cameraWidth, cameraHeight;
        CameraData cameraData = CameraHolder.instance().getCameraData();
        int width = cameraData.cameraWidth;
        int height = cameraData.cameraHeight;
        if(CameraHolder.instance().isLandscape()) {
            cameraWidth = Math.max(width, height);
            cameraHeight = Math.min(width, height);
        } else {
            cameraWidth = Math.min(width, height);
            cameraHeight = Math.max(width, height);
        }
        float hRatio = mVideoWidth / ((float)cameraWidth);
        float vRatio = mVideoHeight / ((float)cameraHeight);

        float ratio;
        if(hRatio > vRatio) {
            ratio = mVideoHeight / (cameraHeight * hRatio);
            final float vtx[] = {
                    //UV
                    0f, 0.5f + ratio/2,
                    0f, 0.5f - ratio/2,
                    1f, 0.5f + ratio/2,
                    1f, 0.5f - ratio/2,
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
            bb.order(ByteOrder.nativeOrder());
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
        } else {
            ratio = mVideoWidth/ (cameraWidth * vRatio);
            final float vtx[] = {
                    //UV
                    0.5f - ratio/2, 1f,
                    0.5f - ratio/2, 0f,
                    0.5f + ratio/2, 1f,
                    0.5f + ratio/2, 0f,
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
            bb.order(ByteOrder.nativeOrder());
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
        }
    }

    public void draw() {
        saveRenderState();
        {
            GlUtil.checkGlError("draw_S");

            if (mRecorder.firstTimeSetup()) {
                mRecorder.startSwapData();
                mRecorder.makeCurrent();
                initGL();
            } else {
                mRecorder.makeCurrent();
            }

            GLES20.glViewport(0, 0, mVideoWidth, mVideoHeight);

            GLES20.glClearColor(0f, 0f, 0f, 1f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);

            mNormalVtxBuf.position(0);
            GLES20.glVertexAttribPointer(maPositionHandle,
                    3, GLES20.GL_FLOAT, false, 4 * 3, mNormalVtxBuf);
            GLES20.glEnableVertexAttribArray(maPositionHandle);

            mCameraTexCoordBuffer.position(0);
            GLES20.glVertexAttribPointer(maTexCoordHandle,
                    2, GLES20.GL_FLOAT, false, 4 * 2, mCameraTexCoordBuffer);
            GLES20.glEnableVertexAttribArray(maTexCoordHandle);

            GLES20.glUniform1i(muSamplerHandle, 0);

            //处理前置摄像头镜像
            CameraData cameraData = CameraHolder.instance().getCameraData();
            if(cameraData != null) {
                int facing = cameraData.cameraFacing;
                if(muPosMtxHandle>= 0) {
                    if(facing == CameraData.FACING_FRONT) {
                        GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mSymmetryMtx, 0);
                    }else {
                        GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mNormalMtx, 0);
                    }
                }
            }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTexId);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            //绘制纹理
            drawWatermark();

            mRecorder.swapBuffers();

            GlUtil.checkGlError("draw_E");
        }
        restoreRenderState();
    }


    private void drawWatermark() {
        if(mWatermarkImg == null) {
            return;
        }
        GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mNormalMtx, 0);

        GLES20.glVertexAttribPointer(maPositionHandle,
                3, GLES20.GL_FLOAT, false, 4 * 3, mWatermarkVertexBuffer);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        mNormalTexCoordBuf.position(0);
        GLES20.glVertexAttribPointer(maTexCoordHandle,
                2, GLES20.GL_FLOAT, false, 4 * 2, mNormalTexCoordBuf);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        if(mWatermarkTextureId == -1) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mWatermarkImg, 0);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            mWatermarkTextureId = textures[0];
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mWatermarkTextureId);

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private void initGL() {
        GlUtil.checkGlError("initGL_S");

        final String vertexShader =
                //
                "attribute vec4 position;\n" +
                        "attribute vec4 inputTextureCoordinate;\n" +
                        "varying   vec2 textureCoordinate;\n" +
                        "uniform   mat4 uPosMtx;\n" +
                        "void main() {\n" +
                        "  gl_Position = uPosMtx * position;\n" +
                        "  textureCoordinate   = inputTextureCoordinate.xy;\n" +
                        "}\n";
        final String fragmentShader =
                //
                "precision mediump float;\n" +
                        "uniform sampler2D uSampler;\n" +
                        "varying vec2 textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(uSampler, textureCoordinate);\n" +
                        "}\n";
        mProgram         = GlUtil.createProgram(vertexShader, fragmentShader);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        muSamplerHandle  = GLES20.glGetUniformLocation(mProgram, "uSampler");
        muPosMtxHandle   = GLES20.glGetUniformLocation(mProgram, "uPosMtx");

        Matrix.scaleM(mSymmetryMtx, 0, -1, 1, 1);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);

        GlUtil.checkGlError("initGL_E");
    }

    private void saveRenderState() {
        mSavedEglDisplay     = EGL14.eglGetCurrentDisplay();
        mSavedEglDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        mSavedEglReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
        mSavedEglContext     = EGL14.eglGetCurrentContext();
    }

    private void restoreRenderState() {
        if (!EGL14.eglMakeCurrent(
                mSavedEglDisplay,
                mSavedEglDrawSurface,
                mSavedEglReadSurface,
                mSavedEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }
}
