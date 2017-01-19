package com.laifeng.sopcastsdk.configuration;

/**
 * @Title: CameraConfiguration
 * @Package ccom.laifeng.sopcastsdk.configuration
 * @Description:
 * @Author Jim
 * @Date 16/9/12
 * @Time 下午3:26
 * @Version
 */
public final class CameraConfiguration {
    public static final int DEFAULT_HEIGHT = 1280;
    public static final int DEFAULT_WIDTH = 720;
    public static final int DEFAULT_FPS = 15;
    public static final Facing DEFAULT_FACING = Facing.FRONT;
    public static final Orientation DEFAULT_ORIENTATION = Orientation.PORTRAIT;
    public static final FocusMode DEFAULT_FOCUSMODE = FocusMode.AUTO;

    public enum  Facing {
        FRONT,
        BACK
    }

    public enum  Orientation {
        LANDSCAPE,
        PORTRAIT
    }

    public enum  FocusMode {
        AUTO,
        TOUCH
    }

    public final int height;
    public final int width;
    public final int fps;
    public final Facing facing;
    public final Orientation orientation;
    public final FocusMode focusMode;


    private CameraConfiguration(final Builder builder) {
        height = builder.height;
        width = builder.width;
        facing = builder.facing;
        fps = builder.fps;
        orientation = builder.orientation;
        focusMode = builder.focusMode;
    }

    public static CameraConfiguration createDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private int height = DEFAULT_HEIGHT;
        private int width = DEFAULT_WIDTH;
        private int fps = DEFAULT_FPS;
        private Facing facing = DEFAULT_FACING;
        private Orientation orientation = DEFAULT_ORIENTATION;
        private FocusMode focusMode = DEFAULT_FOCUSMODE;

        public Builder setPreview(int height, int width) {
            this.height = height;
            this.width = width;
            return this;
        }

        public Builder setFacing(Facing facing) {
            this.facing = facing;
            return this;
        }

        public Builder setOrientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder setFps(int fps) {
            this.fps = fps;
            return this;
        }

        public Builder setFocusMode(FocusMode focusMode) {
            this.focusMode = focusMode;
            return this;
        }

        public CameraConfiguration build() {
            return new CameraConfiguration(this);
        }
    }
}
