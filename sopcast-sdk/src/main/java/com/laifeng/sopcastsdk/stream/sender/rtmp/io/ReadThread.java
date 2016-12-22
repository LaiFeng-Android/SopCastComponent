package com.laifeng.sopcastsdk.stream.sender.rtmp.io;

import android.util.Log;

import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Chunk;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * RTMPConnection's read thread
 * 
 * @author francois, leo
 */
public class ReadThread extends Thread {

    private static final String TAG = "ReadThread";

    private RtmpDecoder rtmpDecoder;
    private InputStream in;
    private OnReadListener listener;
    private volatile boolean startFlag;

    public ReadThread(InputStream in, SessionInfo sessionInfo) {
        this.in = in;
        this.rtmpDecoder = new RtmpDecoder(sessionInfo);
        startFlag = true;
    }

    public void setOnReadListener(OnReadListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        while (startFlag) {
            try {
                Chunk chunk = rtmpDecoder.readPacket(in);
                if(chunk != null && listener != null) {
                    listener.onChunkRead(chunk);
                }
            } catch (EOFException e) {
                startFlag = false;
                if(listener != null) {
                    listener.onStreamEnd();
                }
            } catch (IOException e) {
                startFlag = false;
                if(listener != null) {
                    listener.onDisconnect();
                }
            }
        }
    }

    public void shutdown() {
        listener = null;
        startFlag = false;
        this.interrupt();
    }

    public void clearStoredChunks(int chunkStreamId) {
        rtmpDecoder.clearStoredChunks(chunkStreamId);
    }
}
