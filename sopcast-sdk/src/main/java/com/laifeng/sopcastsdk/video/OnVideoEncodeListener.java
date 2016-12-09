package com.laifeng.sopcastsdk.video;

import android.media.MediaCodec;
import java.nio.ByteBuffer;

/**
 * @Title: OnVideoEncodeListener
 * @Package com.youku.crazytogether.app.modules.sopCastV2.render
 * @Description:
 * @Author Jim
 * @Date 16/4/4
 * @Time 上午10:27
 * @Version
 */
public interface OnVideoEncodeListener {
    void onVideoEncode(ByteBuffer bb, MediaCodec.BufferInfo bi);
}
