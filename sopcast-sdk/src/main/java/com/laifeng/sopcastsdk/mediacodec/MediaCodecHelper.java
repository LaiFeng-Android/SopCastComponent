package com.laifeng.sopcastsdk.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

/**
 * @Title: MediaCodecHelper
 * @Package com.laifeng.sopcastsdk.mediacodec
 * @Description:
 * @Author Jim
 * @Date 2016/10/31
 * @Time 上午11:56
 * @Version
 */

@TargetApi(18)
public class MediaCodecHelper {

    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no match was
     * found.
     */
    public static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}
