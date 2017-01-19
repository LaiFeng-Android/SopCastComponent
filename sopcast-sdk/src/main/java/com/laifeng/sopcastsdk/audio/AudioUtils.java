package com.laifeng.sopcastsdk.audio;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.laifeng.sopcastsdk.configuration.AudioConfiguration;

/**
 * @Title: AudioUtils
 * @Package cn.wecoder.camerademo.audio
 * @Description:
 * @Author Jim
 * @Date 16/3/24
 * @Time 下午5:54
 * @Version
 */
public class AudioUtils {
    public static boolean checkMicSupport(AudioConfiguration audioConfiguration) {
        boolean result;
        int recordBufferSize = getRecordBufferSize(audioConfiguration);
        byte[] mRecordBuffer = new byte[recordBufferSize];
        AudioRecord audioRecord = getAudioRecord(audioConfiguration);
        try {
            audioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int readLen = audioRecord.read(mRecordBuffer, 0, recordBufferSize);
        result = readLen >= 0;
        try {
            audioRecord.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int getRecordBufferSize(AudioConfiguration audioConfiguration) {
        int frequency = audioConfiguration.frequency;
        int audioEncoding = audioConfiguration.encoding;
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        if(audioConfiguration.channelCount == 2) {
            channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        }
        int size = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        return size;
    }

    @TargetApi(18)
    public static AudioRecord getAudioRecord(AudioConfiguration audioConfiguration) {
        int frequency = audioConfiguration.frequency;
        int audioEncoding = audioConfiguration.encoding;
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        if(audioConfiguration.channelCount == 2) {
            channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        }
        int audioSource = MediaRecorder.AudioSource.MIC;
        if(audioConfiguration.aec) {
            audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        }
        AudioRecord audioRecord = new AudioRecord(audioSource, frequency,
                channelConfiguration, audioEncoding, getRecordBufferSize(audioConfiguration));
        return audioRecord;
    }
}
