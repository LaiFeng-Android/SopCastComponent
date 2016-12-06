package com.laifeng.sopcastsdk.controller.video;

import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.video.OnVideoEncodeListener;

/**
 * @Title: IVideoController
 * @Package com.laifeng.sopcastsdk.controller.video
 * @Description:
 * @Author Jim
 * @Date 2016/11/2
 * @Time 下午2:17
 * @Version
 */

public interface IVideoController {
    void start();
    void stop();
    void pause();
    void resume();
    boolean setVideoBps(int bps);
    void setVideoEncoderListener(OnVideoEncodeListener listener);
    void setVideoConfiguration(VideoConfiguration configuration);
}
