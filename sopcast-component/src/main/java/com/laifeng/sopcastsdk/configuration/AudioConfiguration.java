package com.laifeng.sopcastsdk.configuration;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;

/**
 * @Title: AudioConfiguration
 * @Package com.laifeng.sopcastsdk.configuration
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午3:20
 * @Version
 */
public final class AudioConfiguration {
    public static final int DEFAULT_FREQUENCY = 44100;
    public static final int DEFAULT_MAX_BPS = 64;
    public static final int DEFAULT_MIN_BPS = 32;
    public static final int DEFAULT_ADTS = 0;
    public static final String DEFAULT_MIME = "audio/mp4a-latm";
    public static final int DEFAULT_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int DEFAULT_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    public static final int DEFAULT_CHANNEL_COUNT = 1;
    public static final boolean DEFAULT_AEC = false;

    public final int minBps;
    public final int maxBps;
    public final int frequency;
    public final int encoding;
    public final int channelCount;
    public final int adts;
    public final int aacProfile;
    public final String mime;
    public final boolean aec;

    private AudioConfiguration(final Builder builder) {
        minBps = builder.minBps;
        maxBps = builder.maxBps;
        frequency = builder.frequency;
        encoding = builder.encoding;
        channelCount = builder.channelCount;
        adts = builder.adts;
        mime = builder.mime;
        aacProfile = builder.aacProfile;
        aec = builder.aec;
    }

    public static AudioConfiguration createDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private int minBps = DEFAULT_MIN_BPS;
        private int maxBps = DEFAULT_MAX_BPS;
        private int frequency = DEFAULT_FREQUENCY;
        private int encoding = DEFAULT_AUDIO_ENCODING;
        private int channelCount = DEFAULT_CHANNEL_COUNT;
        private int adts = DEFAULT_ADTS;
        private String mime = DEFAULT_MIME;
        private int aacProfile = DEFAULT_AAC_PROFILE;
        private boolean aec = DEFAULT_AEC;

        public Builder setBps(int minBps, int maxBps) {
            this.minBps = minBps;
            this.maxBps = maxBps;
            return this;
        }

        public Builder setFrequency(int frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder setEncoding(int encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder setChannelCount(int channelCount) {
            this.channelCount = channelCount;
            return this;
        }

        public Builder setAdts(int adts) {
            this.adts = adts;
            return this;
        }

        public Builder setAacProfile(int aacProfile) {
            this.aacProfile = aacProfile;
            return this;
        }

        public Builder setMime(String mime) {
            this.mime = mime;
            return this;
        }

        public Builder setAec(boolean aec) {
            this.aec = aec;
            return this;
        }

        public AudioConfiguration build() {
            return new AudioConfiguration(this);
        }
    }
}
