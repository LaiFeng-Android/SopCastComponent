package com.laifeng.sopcastsdk.video.effect;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;

import com.laifeng.sopcastsdk.camera.CameraData;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.GLSLFileUtils;
import com.laifeng.sopcastsdk.video.GlUtil;

import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * @Title: Effect
 * @Package com.laifeng.sopcastsdk.video.effert
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:10
 * @Version
 */
public abstract class Effect {
    private final FloatBuffer mVtxBuf = GlUtil.createSquareVtx();
    private final float[]     mPosMtx = GlUtil.createIdentityMtx();

    protected int mTextureId = -1;
    private int mProgram            = -1;
    private int maPositionHandle    = -1;
    private int maTexCoordHandle    = -1;
    private int muPosMtxHandle      = -1;
    private int muTexMtxHandle      = -1;

    private final int[]       mFboId  = new int[]{0};
    private final int[]       mRboId  = new int[]{0};
    private final int[]       mTexId  = new int[]{0};

    private int mWidth  = -1;
    private int mHeight = -1;
    private float mAngle = 270;

    private final LinkedList<Runnable> mRunOnDraw;
    private String mVertex;
    private String mFragment;

    public Effect() {
        mRunOnDraw = new LinkedList<>();
    }

    public void setShader(String vertex, String fragment) {
        mVertex = vertex;
        mFragment = fragment;
    }

    public void prepare() {
        loadShaderAndParams(mVertex, mFragment);
        initSize();
        createEffectTexture();
    }

    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    private void loadShaderAndParams(String vertex, String fragment) {
        if(TextUtils.isEmpty(vertex) || TextUtils.isEmpty(fragment)) {
            vertex = SopCastConstant.SHARDE_NULL_VERTEX;
            fragment = SopCastConstant.SHARDE_NULL_FRAGMENT;
            SopCastLog.e(SopCastConstant.TAG, "Couldn't load the shader, so use the null shader!");
        }
        GlUtil.checkGlError("initSH_S");
        mProgram = GlUtil.createProgram(vertex, fragment);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");

        muPosMtxHandle   = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        muTexMtxHandle   = GLES20.glGetUniformLocation(mProgram, "uTexMtx");
        loadOtherParams();
        GlUtil.checkGlError("initSH_E");
    }

    protected void loadOtherParams() {
        //do nothing
    }

    private void initSize() {
        if(CameraHolder.instance().getState() != CameraHolder.State.PREVIEW) {
            return;
        }
        CameraData cameraData = CameraHolder.instance().getCameraData();
        int width = cameraData.cameraWidth;
        int height = cameraData.cameraHeight;
        if(CameraHolder.instance().isLandscape()) {
            mWidth = Math.max(width, height);
            mHeight = Math.min(width, height);
        } else {
            mWidth = Math.min(width, height);
            mHeight = Math.max(width, height);
        }

    }

    private void createEffectTexture() {
        if(CameraHolder.instance().getState() != CameraHolder.State.PREVIEW) {
            return;
        }
        GlUtil.checkGlError("initFBO_S");
        GLES20.glGenFramebuffers(1, mFboId, 0);
        GLES20.glGenRenderbuffers(1, mRboId, 0);
        GLES20.glGenTextures(1, mTexId, 0);

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRboId[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
                GLES20.GL_DEPTH_COMPONENT16, mWidth, mHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRboId[0]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mTexId[0], 0);

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) !=
                GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("glCheckFramebufferStatus()");
        }
        GlUtil.checkGlError("initFBO_E");
    }

    public int getEffertedTextureId() {
        return mTexId[0];
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    public void draw(final float[] tex_mtx) {
        if (-1 == mProgram || mTextureId == -1 || mWidth == -1) {
            return;
        }

        GlUtil.checkGlError("draw_S");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);

        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        runPendingOnDrawTasks();

        mVtxBuf.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle,
                3, GLES20.GL_FLOAT, false, 4 * (3 + 2), mVtxBuf);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        mVtxBuf.position(3);
        GLES20.glVertexAttribPointer(maTexCoordHandle,
                2, GLES20.GL_FLOAT, false, 4 * (3 + 2), mVtxBuf);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        if(muPosMtxHandle>= 0)
            GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);

        if(muTexMtxHandle>= 0)
            GLES20.glUniformMatrix4fv(muTexMtxHandle, 1, false, tex_mtx, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtil.checkGlError("draw_E");
    }

    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    public void release() {
        if (-1 != mProgram) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = -1;
        }
    }
}
