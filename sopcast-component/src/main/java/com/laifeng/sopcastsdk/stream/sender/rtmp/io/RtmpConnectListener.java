package com.laifeng.sopcastsdk.stream.sender.rtmp.io;

/**
 * @Title: RtmpConnectListener
 * @Package com.jimfengfly.rtmppublisher.io
 * @Description:
 * @Author Jim
 * @Date 2016/11/30
 * @Time 下午2:37
 * @Version
 */

public interface RtmpConnectListener {
    void onUrlInvalid();
    void onSocketConnectSuccess();
    void onSocketConnectFail();
    void onHandshakeSuccess();
    void onHandshakeFail();
    void onRtmpConnectSuccess();
    void onRtmpConnectFail();
    void onCreateStreamSuccess();
    void onCreateStreamFail();
    void onPublishSuccess();
    void onPublishFail();
    void onSocketDisconnect();
    void onStreamEnd();
}
