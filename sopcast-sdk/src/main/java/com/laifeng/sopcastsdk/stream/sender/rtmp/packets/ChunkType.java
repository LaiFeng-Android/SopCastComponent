package com.laifeng.sopcastsdk.stream.sender.rtmp.packets;

import com.laifeng.sopcastsdk.stream.sender.rtmp.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * @Title: ChunkType
 * @Package com.laifeng.sopcastsdk.stream.sender.rtmp.packets
 * @Description:
 * @Author Jim
 * @Date 2016/12/2
 * @Time 下午3:11
 * @Version
 */

public enum  ChunkType {
    /** Full 11-byte RTMP chunk header */
    TYPE_0_FULL(0x00),
    /** Relative 7-byte RTMP chunk header (message stream ID is not included) */
    TYPE_1_LARGE(0x01),
    /** Relative 3-byte RTMP chunk header (only timestamp delta) */
    TYPE_2_TIMESTAMP_ONLY(0x02),
    /** Relative 0-byte RTMP chunk header (no "real" header, just the 1-byte indicating chunk header type & chunk stream ID) */
    TYPE_3_NO_BYTE(0x03);
    /** The byte value of this chunk header type */
    private byte value;
    /** The full size (in bytes) of this RTMP header (including the basic header byte) */
    private static final Map<Byte, ChunkType> quickLookupMap = new HashMap<>();

    static {
        for (ChunkType messageTypId : ChunkType.values()) {
            quickLookupMap.put(messageTypId.getValue(), messageTypId);
        }
    }

    ChunkType(int byteValue) {
        this.value = (byte) byteValue;
    }

    /** Returns the byte value of this chunk header type */
    public byte getValue() {
        return value;
    }

    public static ChunkType valueOf(byte chunkHeaderType) {
        if (quickLookupMap.containsKey(chunkHeaderType)) {
            return quickLookupMap.get(chunkHeaderType);
        } else {
            throw new IllegalArgumentException("Unknown chunk header type byte: " + Util.toHexString(chunkHeaderType));
        }
    }
}
