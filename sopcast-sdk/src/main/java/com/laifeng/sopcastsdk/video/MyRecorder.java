package com.laifeng.sopcastsdk.video;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.mediacodec.VideoMediaCodec;
import com.laifeng.sopcastsdk.utils.SopCastLog;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(18)
public class MyRecorder {
	private MediaCodec mMediaCodec;
	private InputSurface mInputSurface;
	private OnVideoEncodeListener mListener;
	private boolean mPause;
	private MediaCodec.BufferInfo mBufferInfo;
	private VideoConfiguration mConfiguration;
	private HandlerThread mHandlerThread;
	private Handler mEncoderHandler;
	private ReentrantLock encodeLock = new ReentrantLock();
	private volatile boolean isStarted;

	public MyRecorder(VideoConfiguration configuration) {
		mConfiguration = configuration;
	}

	public void setVideoEncodeListener(OnVideoEncodeListener listener) {
		mListener = listener;
	}

	public void setPause(boolean pause) {
		mPause = pause;
	}

	public void prepareEncoder() {
		if (mMediaCodec != null || mInputSurface != null) {
			throw new RuntimeException("prepareEncoder called twice?");
		}
		mMediaCodec = VideoMediaCodec.getVideoMediaCodec(mConfiguration);
		mHandlerThread = new HandlerThread("SopCastEncode");
		mHandlerThread.start();
		mEncoderHandler = new Handler(mHandlerThread.getLooper());
		mBufferInfo = new MediaCodec.BufferInfo();
		isStarted = true;
	}

	public boolean firstTimeSetup() {
		if (mMediaCodec == null || mInputSurface != null) {
			return false;
		}
		try {
			mInputSurface = new InputSurface(mMediaCodec.createInputSurface());
			mMediaCodec.start();
		} catch (Exception e) {
			releaseEncoder();
			throw (RuntimeException)e;
		}
		return true;
	}

	public void startSwapData() {
		mEncoderHandler.post(swapDataRunnable);
	}

	public void makeCurrent() {
		mInputSurface.makeCurrent();
	}

	public void swapBuffers() {
		if (mMediaCodec == null || mPause) {
			return;
		}
		mInputSurface.swapBuffers();
		mInputSurface.setPresentationTime(System.nanoTime());
	}

	private Runnable swapDataRunnable = new Runnable() {
		@Override
		public void run() {
			drainEncoder();
		}
	};

	public void stop() {
		if (!isStarted) {
			return;
		}
		isStarted = false;
		mEncoderHandler.removeCallbacks(null);
		mHandlerThread.quit();
		encodeLock.lock();
		releaseEncoder();
		encodeLock.unlock();
	}

	private void releaseEncoder() {
		if (mMediaCodec != null) {
			mMediaCodec.signalEndOfInputStream();
			mMediaCodec.stop();
			mMediaCodec.release();
			mMediaCodec = null;
		}
		if (mInputSurface != null) {
			mInputSurface.release();
			mInputSurface = null;
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public boolean setRecorderBps(int bps) {
		if (mMediaCodec == null || mInputSurface == null) {
			return false;
		}
		SopCastLog.d(SopCastConstant.TAG, "bps :" + bps * 1024);
		Bundle bitrate = new Bundle();
		bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024);
		mMediaCodec.setParameters(bitrate);
		return true;
	}

	private void drainEncoder() {
		ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
		while (isStarted) {
			encodeLock.lock();
			if(mMediaCodec != null) {
				int outBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 12000);
				if (outBufferIndex >= 0) {
					ByteBuffer bb = outBuffers[outBufferIndex];
					if (mListener != null) {
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
