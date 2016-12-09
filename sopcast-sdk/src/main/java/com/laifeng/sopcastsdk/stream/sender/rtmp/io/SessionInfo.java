package com.laifeng.sopcastsdk.stream.sender.rtmp.io;

import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.ChunkHeader;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Title: SessionInfo
 * @Package com.jimfengfly.rtmppublisher.packets
 * @Description:
 * @Author Jim
 * @Date 2016/11/29
 * @Time 上午10:49
 * @Version
 */

public class SessionInfo {

    public static final byte RTMP_STREAM_CHANNEL = 0x05;
    public static final byte RTMP_COMMAND_CHANNEL = 0x03;
    public static final byte RTMP_VIDEO_CHANNEL = 0x06;
    public static final byte RTMP_AUDIO_CHANNEL = 0x07;
    public static final byte RTMP_CONTROL_CHANNEL = 0x02;

    /** The window acknowledgement size for this RTMP session, in bytes; default to max to avoid unnecessary "Acknowledgment" messages from being sent */
    private int acknowledgementWindowSize = Integer.MAX_VALUE;
    /** Used internally to store the total number of bytes read (used when sending Acknowledgement messages) */
    private int totalBytesRead = 0;

    /** Default chunk size is 128 bytes */
    private int rxChunkSize = 128;
    private int txChunkSize = 128;
    private Map<Integer, ChunkHeader> chunkReceiveChannels = new HashMap<>();
    private Map<Integer, ChunkHeader> chunkSendChannels = new HashMap<>();
    private Map<Integer, String> invokedMethods = new ConcurrentHashMap<>();

    public ChunkHeader getPreReceiveChunkHeader(int chunkStreamId) {
        return chunkReceiveChannels.get(chunkStreamId);
    }

    public void putPreReceiveChunkHeader(int chunkStreamId, ChunkHeader chunkHeader) {
        chunkReceiveChannels.put(chunkStreamId, chunkHeader);
    }

    public ChunkHeader getPreSendChunkHeader(int chunkStreamId) {
        return chunkSendChannels.get(chunkStreamId);
    }

    public void putPreSendChunkHeader(int chunkStreamId, ChunkHeader chunkHeader) {
        chunkSendChannels.put(chunkStreamId, chunkHeader);
    }

    public String takeInvokedCommand(int transactionId) {
        return invokedMethods.remove(transactionId);
    }

    public String addInvokedCommand(int transactionId, String commandName) {
        return invokedMethods.put(transactionId, commandName);
    }

    public int getRxChunkSize() {
        return rxChunkSize;
    }

    public void setRxChunkSize(int chunkSize) {
        this.rxChunkSize = chunkSize;
    }

    public int getTxChunkSize() {
        return txChunkSize;
    }

    public void setTxChunkSize(int chunkSize) {
        this.txChunkSize = chunkSize;
    }

    public int getAcknowledgementWindowSize() {
        return acknowledgementWindowSize;
    }

    public void setAcknowledgmentWindowSize(int acknowledgementWindowSize) {
        this.acknowledgementWindowSize = acknowledgementWindowSize;
    }

    private static long sessionBeginTimestamp;

    /** Sets the session beginning timestamp for all chunks */
    public static void markSessionTimestampTx() {
        sessionBeginTimestamp = System.nanoTime() / 1000000;
    }

    /** Utility method for calculating & synchronizing transmitted timestamps */
    public long markAbsoluteTimestampTx() {
        return System.nanoTime() / 1000000 - sessionBeginTimestamp;
    }
}
