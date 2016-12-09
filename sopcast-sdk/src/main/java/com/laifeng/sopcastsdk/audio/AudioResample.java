package com.laifeng.sopcastsdk.audio;

/**
 * @Title: AudioResample
 * @Package com.android.webrtc.audio
 * @Description:
 * @Author Jim
 * @Date 16/8/19
 * @Time 下午4:55
 * @Version
 */
public class AudioResample {

    static {
        System.loadLibrary("webrtc_resample"); // to load the libwebrtc_aecm.so library.
    }

    private static native void nativeInit();

    private static native void nativeResample16khzTo48khz(short[] in, short[] out);

    private static native void nativeClose();

    public void init() {
        nativeInit();
    }

    public void resample16khzTo48khz(short[] in, short[] out) {
        nativeResample16khzTo48khz(in, out);
    }

    public void close() {
        nativeClose();
    }
}
