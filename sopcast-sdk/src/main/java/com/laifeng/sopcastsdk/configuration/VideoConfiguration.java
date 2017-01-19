package com.laifeng.sopcastsdk.configuration;

/**
 * @Title: VideoConfiguration
 * @Package com.laifeng.sopcastsdk.configuration
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午3:20
 * @Version
 */
public final class VideoConfiguration {
    public static final int DEFAULT_HEIGHT = 640;
    public static final int DEFAULT_WIDTH = 360;
    public static final int DEFAULT_FPS = 15;
    public static final int DEFAULT_MAX_BPS = 1300;
    public static final int DEFAULT_MIN_BPS = 400;
    public static final int DEFAULT_IFI = 2;
    public static final String DEFAULT_MIME = "video/avc";

    public final int height;
    public final int width;
    public final int minBps;
    public final int maxBps;
    public final int fps;
    public final int ifi;
    public final String mime;

    private VideoConfiguration(final Builder builder) {
        height = builder.height;
        width = builder.width;
        minBps = builder.minBps;
        maxBps = builder.maxBps;
        fps = builder.fps;
        ifi = builder.ifi;
        mime = builder.mime;
    }

    public static VideoConfiguration createDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private int height = DEFAULT_HEIGHT;
        private int width = DEFAULT_WIDTH;
        private int minBps = DEFAULT_MIN_BPS;
        private int maxBps = DEFAULT_MAX_BPS;
        private int fps = DEFAULT_FPS;
        private int ifi = DEFAULT_IFI;
        private String mime = DEFAULT_MIME;

        public Builder setSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder setBps(int minBps, int maxBps) {
            this.minBps = minBps;
            this.maxBps = maxBps;
            return this;
        }

        public Builder setFps(int fps) {
            this.fps = fps;
            return this;
        }

        public Builder setIfi(int ifi) {
            this.ifi = ifi;
            return this;
        }

        public Builder setMime(String mime) {
            this.mime = mime;
            return this;
        }

        public VideoConfiguration build() {
            return new VideoConfiguration(this);
        }
    }
}
