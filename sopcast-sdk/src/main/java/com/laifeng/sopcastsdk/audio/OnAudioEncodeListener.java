package com.laifeng.sopcastsdk.audio;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * @Title: OnAudioEncodeListener
 * @Package com.youku.crazytogether.app.modules.livehouse_new.widget.sopCastV2.audio
 * @Description:
 * @Author Jim
 * @Date 16/4/4
 * @Time 上午10:24
 * @Version
 */
public interface OnAudioEncodeListener {
    void onAudioEncode(ByteBuffer bb, MediaCodec.BufferInfo bi);
}
