package com.laifeng.sopcastsdk.screen;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.mediacodec.VideoMediaCodec;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.OnVideoEncodeListener;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * @Title: ScreenRecordEncoder
 * @Package com.laifeng.sopcastsdk.screen
 * @Description:
 * @Author Jim
 * @Date 2016/11/2
 * @Time 下午3:36
 * @Version
 */
@TargetApi(LOLLIPOP)
public class ScreenRecordEncoder {
    private MediaCodec mMediaCodec;
    private OnVideoEncodeListener mListener;
    private boolean mPause;
    private HandlerThread mHandlerThread;
    private Handler mEncoderHandler;
    private VideoConfiguration mConfiguration;
    private MediaCodec.BufferInfo mBufferInfo;
    private volatile boolean isStarted;
    private ReentrantLock encodeLock = new ReentrantLock();

    public ScreenRecordEncoder(VideoConfiguration configuration) {
        mConfiguration = configuration;
        mMediaCodec = VideoMediaCodec.getVideoMediaCodec(mConfiguration);
    }

    public void setOnVideoEncodeListener(OnVideoEncodeListener listener) {
        mListener = listener;
    }

    public void setPause(boolean pause) {
        mPause = pause;
    }

    public void start() {
        mHandlerThread = new HandlerThread("LFEncode");
        mHandlerThread.start();
        mEncoderHandler = new Handler(mHandlerThread.getLooper());
        mBufferInfo = new MediaCodec.BufferInfo();
        mMediaCodec.start();
        mEncoderHandler.post(swapDataRunnable);
        isStarted = true;
    }

    public Surface getSurface() {
        if(mMediaCodec != null) {
            return mMediaCodec.createInputSurface();
        } else {
            return null;
        }
    }

    private Runnable swapDataRunnable = new Runnable() {
        @Override
        public void run() {
            drainEncoder();
        }
    };

    public void stop() {
        isStarted = false;
        mEncoderHandler.removeCallbacks(null);
        mHandlerThread.quit();
        encodeLock.lock();
        mMediaCodec.signalEndOfInputStream();
        releaseEncoder();
        encodeLock.unlock();
    }

    private void releaseEncoder() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setRecorderBps(int bps) {
        if (mMediaCodec == null) {
            return;
        }
        SopCastLog.d(SopCastConstant.TAG, "bps :" + bps * 1024);
        Bundle bitrate = new Bundle();
        bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024);
        mMediaCodec.setParameters(bitrate);
    }

    private void drainEncoder() {
        ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
        while (isStarted) {
            encodeLock.lock();
            if(mMediaCodec != null) {
                int outBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);
                if (outBufferIndex >= 0) {
                    ByteBuffer bb = outBuffers[outBufferIndex];
                    if (mListener != null && !mPause) {
                        mListener.onVideoEncode(bb, mBufferInfo);
                    }
                    mMediaCodec.releaseOutputBuffer(outBufferIndex, false);
                } else {
                    try {
                        // wait 10ms
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                encodeLock.unlock();
            } else {
                encodeLock.unlock();
                break;
            }
        }
    }
}
