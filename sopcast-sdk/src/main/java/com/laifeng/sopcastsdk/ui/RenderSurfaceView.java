package com.laifeng.sopcastsdk.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.MyRenderer;
import com.laifeng.sopcastsdk.video.effect.Effect;

/**
 * @Title: RenderSurfaceView
 * @Package com.laifeng.sopcastsdk.ui
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午5:12
 * @Version
 */
public class RenderSurfaceView extends GLSurfaceView {
    private MyRenderer mRenderer;

    public RenderSurfaceView(Context context) {
        super(context);
        init();
    }

    public RenderSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mRenderer = new MyRenderer(this);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(mSurfaceHolderCallback);
    }

    public MyRenderer getRenderer() {
        return mRenderer;
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            SopCastLog.d(SopCastConstant.TAG, "SurfaceView destroy");
            CameraHolder.instance().stopPreview();
            CameraHolder.instance().releaseCamera();
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            SopCastLog.d(SopCastConstant.TAG, "SurfaceView created");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            SopCastLog.d(SopCastConstant.TAG, "SurfaceView width:" + width + " height:" + height);
        }
    };

    public void setEffect(final Effect effect) {
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != mRenderer) {
                    mRenderer.setEffect(effect);
                }
            }
        });
    }
}
