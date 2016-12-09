package com.laifeng.sopcastsdk.controller.video;

import android.annotation.TargetApi;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.view.Surface;

import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.controller.video.IVideoController;
import com.laifeng.sopcastsdk.mediacodec.VideoMediaCodec;
import com.laifeng.sopcastsdk.screen.ScreenRecordEncoder;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.OnVideoEncodeListener;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * @Title: ScreenVideoController
 * @Package com.laifeng.sopcastsdk.controller.video
 * @Description:
 * @Author Jim
 * @Date 2016/11/2
 * @Time 下午2:20
 * @Version
 */
@TargetApi(LOLLIPOP)
public class ScreenVideoController implements IVideoController {
    private MediaProjectionManager mManager;
    private int resultCode;
    private Intent resultData;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection mMediaProjection;
    private VideoConfiguration mVideoConfiguration = VideoConfiguration.createDefault();
    private ScreenRecordEncoder mEncoder;
    private OnVideoEncodeListener mListener;

    public ScreenVideoController (MediaProjectionManager manager, int resultCode, Intent resultData) {
        mManager = manager;
        this.resultCode = resultCode;
        this.resultData = resultData;
    }

    @Override
    public void start() {
        mEncoder = new ScreenRecordEncoder(mVideoConfiguration);
        Surface surface = mEncoder.getSurface();
        mEncoder.start();
        mEncoder.setOnVideoEncodeListener(mListener);
        mMediaProjection = mManager.getMediaProjection(resultCode, resultData);
        int width = VideoMediaCodec.getVideoSize(mVideoConfiguration.width);
        int height = VideoMediaCodec.getVideoSize(mVideoConfiguration.height);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenRecoder",
                width, height, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null);
    }

    @Override
    public void stop() {
        if(mEncoder != null) {
            mEncoder.setOnVideoEncodeListener(null);
            mEncoder.stop();
            mEncoder = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    @Override
    public void pause() {
        if(mEncoder != null) {
            mEncoder.setPause(true);
        }
    }

    @Override
    public void resume() {
        if(mEncoder != null) {
            mEncoder.setPause(false);
        }
    }

    @Override
    public boolean setVideoBps(int bps) {
        //重新设置硬编bps，在低于19的版本需要重启编码器
        boolean result = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //由于重启硬编编码器效果不好，此次不做处理
            SopCastLog.d(SopCastConstant.TAG, "Bps need change, but MediaCodec do not support.");
        }else {
            if (mEncoder != null) {
                SopCastLog.d(SopCastConstant.TAG, "Bps change, current bps: " + bps);
                mEncoder.setRecorderBps(bps);
                result = true;
            }
        }
        return result;
    }

    @Override
    public void setVideoEncoderListener(OnVideoEncodeListener listener) {
        mListener = listener;
    }

    @Override
    public void setVideoConfiguration(VideoConfiguration configuration) {
        mVideoConfiguration = configuration;
    }
}
