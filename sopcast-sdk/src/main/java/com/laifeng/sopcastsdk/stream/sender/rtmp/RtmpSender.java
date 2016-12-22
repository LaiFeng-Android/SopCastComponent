package com.laifeng.sopcastsdk.stream.sender.rtmp;

import com.laifeng.sopcastsdk.stream.packer.rtmp.RtmpPacker;
import com.laifeng.sopcastsdk.stream.sender.Sender;
import com.laifeng.sopcastsdk.stream.sender.rtmp.io.RtmpConnectListener;
import com.laifeng.sopcastsdk.stream.sender.rtmp.io.RtmpConnection;
import com.laifeng.sopcastsdk.stream.sender.sendqueue.ISendQueue;
import com.laifeng.sopcastsdk.stream.sender.sendqueue.NormalSendQueue;
import com.laifeng.sopcastsdk.stream.sender.sendqueue.SendQueueListener;
import com.laifeng.sopcastsdk.utils.WeakHandler;

/**
 * @Title: RtmpSender
 * @Package com.laifeng.sopcastsdk.stream.sender.rtmp
 * @Description:
 * @Author Jim
 * @Date 16/9/21
 * @Time 上午11:16
 * @Version
 */
public class RtmpSender implements Sender, SendQueueListener {
    private RtmpConnection rtmpConnection;
    private String mRtmpUrl;
    private OnSenderListener mListener;
    private WeakHandler mHandler = new WeakHandler();
    private ISendQueue mSendQueue = new NormalSendQueue();

    @Override
    public void good() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mListener != null) {
                    mListener.onNetGood();
                }
            }
        });
    }

    @Override
    public void bad() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mListener != null) {
                    mListener.onNetBad();
                }
            }
        });
    }

    public interface OnSenderListener {
        void onConnecting();
        void onConnected();
        void onDisConnected();
        void onPublishFail();
        void onNetGood();
        void onNetBad();
    }

    public RtmpSender() {
        rtmpConnection = new RtmpConnection();
    }

    public void setAddress(String url) {
        mRtmpUrl = url;
    }

    public void setSendQueue(ISendQueue sendQueue) {
        mSendQueue = sendQueue;
    }

    public void setVideoParams(int width, int height) {
        rtmpConnection.setVideoParams(width, height);
    }

    public void setAudioParams(int sampleRate, int sampleSize, boolean isStereo) {
        rtmpConnection.setAudioParams(sampleRate, sampleSize, isStereo);
    }

    public void setSenderListener(OnSenderListener listener) {
        mListener = listener;
    }

    public void connect() {
        rtmpConnection.setSendQueue(mSendQueue);
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectNotInUi();
            }
        }).start();
        if(mListener != null) {
            mListener.onConnecting();
        }
    }

    private synchronized void connectNotInUi() {
        rtmpConnection.setConnectListener(listener);
        rtmpConnection.connect(mRtmpUrl);
    }


    @Override
    public synchronized void start() {
        mSendQueue.setSendQueueListener(this);
        mSendQueue.start();
    }

    @Override
    public void onData(byte[] data, int type) {
        if(type == RtmpPacker.FIRST_AUDIO || type == RtmpPacker.AUDIO) {
            rtmpConnection.publishAudioData(data, type);
        } else if(type == RtmpPacker.FIRST_VIDEO ||
                type == RtmpPacker.INTER_FRAME || type == RtmpPacker.KEY_FRAME) {
            rtmpConnection.publishVideoData(data, type);
        }
    }

    @Override
    public synchronized void stop() {
        rtmpConnection.stop();
        rtmpConnection.setConnectListener(null);
        mSendQueue.setSendQueueListener(null);
        mSendQueue.stop();
    }

    private RtmpConnectListener listener = new RtmpConnectListener() {
        @Override
        public void onUrlInvalid() {
            sendPublishFail();
        }

        @Override
        public void onSocketConnectSuccess() {

        }

        @Override
        public void onSocketConnectFail() {
            sendPublishFail();
        }

        @Override
        public void onHandshakeSuccess() {

        }

        @Override
        public void onHandshakeFail() {
            sendPublishFail();
        }

        @Override
        public void onRtmpConnectSuccess() {

        }

        @Override
        public void onRtmpConnectFail() {
            sendPublishFail();
        }

        @Override
        public void onCreateStreamSuccess() {

        }

        @Override
        public void onCreateStreamFail() {
            sendPublishFail();
        }

        @Override
        public void onPublishSuccess() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mListener != null) {
                        mListener.onConnected();
                    }
                }
            });
        }

        @Override
        public void onPublishFail() {
            sendPublishFail();
        }

        @Override
        public void onSocketDisconnect() {
            sendDisconnectMsg();
        }

        @Override
        public void onStreamEnd() {
            sendDisconnectMsg();
        }
    };

    public void sendDisconnectMsg() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mListener != null) {
                    mListener.onDisConnected();
                }
            }
        });
    }

    public void sendPublishFail() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mListener != null) {
                    mListener.onPublishFail();
                }
            }
        });
    }
}
