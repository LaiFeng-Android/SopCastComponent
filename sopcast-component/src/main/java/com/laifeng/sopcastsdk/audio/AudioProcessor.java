package com.laifeng.sopcastsdk.audio;

import android.media.AudioRecord;

import com.laifeng.sopcastsdk.configuration.AudioConfiguration;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * @Title: AudioProcessor
 * @Package com.laifeng.sopcastsdk.audio
 * @Description:
 * @Author Jim
 * @Date 16/9/19
 * @Time 上午9:56
 * @Version
 */
public class AudioProcessor extends Thread {
    private volatile boolean mPauseFlag;
    private volatile boolean mStopFlag;
    private volatile boolean mMute;
    private AudioRecord mAudioRecord;
    private AudioEncoder mAudioEncoder;
    private byte[] mRecordBuffer;
    private int mRecordBufferSize;
    private AudioConfiguration mAudioConfiguration;
    private AudioResample mResampler;

    public AudioProcessor(AudioRecord audioRecord, AudioConfiguration audioConfiguration) {
        mRecordBufferSize = AudioUtils.getRecordBufferSize(audioConfiguration);
        mRecordBuffer =  new byte[mRecordBufferSize];
        mAudioRecord = audioRecord;
        mAudioConfiguration = audioConfiguration;
        mResampler = new AudioResample();
        mResampler.init();
        mAudioEncoder = new AudioEncoder(audioConfiguration);
        mAudioEncoder.prepareEncoder();
    }

    public void setAudioHEncodeListener(OnAudioEncodeListener listener) {
        mAudioEncoder.setOnAudioEncodeListener(listener);
    }

    public void stopEncode() {
        mStopFlag = true;
        if(mResampler != null) {
            mResampler.close();
            mResampler = null;
        }
        if(mAudioEncoder != null) {
            mAudioEncoder.stop();
            mAudioEncoder = null;
        }
    }

    public void pauseEncode(boolean pause) {
        mPauseFlag = pause;
    }

    public void setMute(boolean mute) {
        mMute = mute;
    }

    public void run() {
        while (!mStopFlag) {
            while (mPauseFlag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int readLen = mAudioRecord.read(mRecordBuffer, 0, mRecordBufferSize);
            if (readLen > 0) {
                if(mAudioConfiguration.aec && mAudioConfiguration.frequency == 48000) {
                    // need resample
                    byte[] outBuf = new byte[mRecordBufferSize*3];
                    ShortBuffer inShortBuff = ByteBuffer.wrap(mRecordBuffer).order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer();
                    ShortBuffer outShortBuff = ByteBuffer.wrap(outBuf).order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer();
                    if(mMute) {
                        byte clearM = 0;
                        Arrays.fill(outBuf, clearM);
                    } else {
                        for(int i=0;i<mRecordBufferSize/320;i++) {
                            short[] inShort = new short[160];
                            short[] outShort = new short[480];
                            inShortBuff.get(inShort);
                            if(mResampler == null) {
                                return;
                            }
                            mResampler.resample16khzTo48khz(inShort, outShort);
                            outShortBuff.put(outShort);
                        }
                    }
                    if(mAudioEncoder != null) {
                        mAudioEncoder.offerEncoder(outBuf);
                    }
                } else {
                    if (mMute) {
                        byte clearM = 0;
                        Arrays.fill(mRecordBuffer, clearM);
                    }
                    if(mAudioEncoder != null) {
                        mAudioEncoder.offerEncoder(mRecordBuffer);
                    }
                }
            }
        }
    }
}
