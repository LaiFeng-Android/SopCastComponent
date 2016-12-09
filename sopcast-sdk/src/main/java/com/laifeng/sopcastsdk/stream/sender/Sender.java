package com.laifeng.sopcastsdk.stream.sender;

/**
 * @Title: Sender
 * @Package com.laifeng.sopcastsdk.stream.sender.rtmp
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 上午11:25
 * @Version
 */
public interface Sender {
    void start();
    void onData(byte[] data, int type);
    void stop();
}
