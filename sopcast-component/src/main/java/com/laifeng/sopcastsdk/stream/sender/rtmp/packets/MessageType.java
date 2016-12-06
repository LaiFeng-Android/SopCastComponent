package com.laifeng.sopcastsdk.stream.sender.rtmp.packets;

import com.laifeng.sopcastsdk.stream.sender.rtmp.Util;
import java.util.HashMap;
import java.util.Map;

/**
 * @Title: MessageType
 * @Package com.laifeng.sopcastsdk.stream.sender.rtmp.packets
 * @Description:
 * RTMP packet/message type definitions.
 * Note: docstrings are adapted from the official Adobe RTMP spec:
 * http://www.adobe.com/devnet/rtmp/
 * @Author Jim
 * @Date 2016/12/2
 * @Time 下午3:07
 * @Version
 */

public enum MessageType {
    /**
     * Protocol control message 1
     * Set Chunk Size, is used to notify the peer a new maximum chunk size to use.
     */
    SET_CHUNK_SIZE(0x01),
    /**
     * Protocol control message 2
     * Abort Message, is used to notify the peer if it is waiting for chunks
     * to complete a message, then to discard the partially received message
     * over a chunk stream and abort processing of that message.
     */
    ABORT(0x02),
    /**
     * Protocol control message 3
     * The client or the server sends the acknowledgment to the peer after
     * receiving bytes equal to the window size. The window size is the
     * maximum number of bytes that the sender sends without receiving
     * acknowledgment from the receiver.
     */
    ACKNOWLEDGEMENT(0x03),
    /**
     * Protocol control message 4
     * The client or the server sends this message to notify the peer about
     * the user control events. This message carries Event type and Event
     * data.
     * Also known as a PING message in some RTMP implementations.
     */
    USER_CONTROL_MESSAGE(0x04),
    /**
     * Protocol control message 5
     * The client or the server sends this message to inform the peer which
     * window size to use when sending acknowledgment.
     * Also known as ServerBW ("server bandwidth") in some RTMP implementations.
     */
    WINDOW_ACKNOWLEDGEMENT_SIZE(0x05),
    /**
     * Protocol control message 6
     * The client or the server sends this message to update the output
     * bandwidth of the peer. The output bandwidth value is the same as the
     * window size for the peer.
     * Also known as ClientBW ("client bandwidth") in some RTMP implementations.
     */
    SET_PEER_BANDWIDTH(0x06),
    /**
     * RTMP audio packet (0x08)
     * The client or the server sends this message to send audio data to the peer.
     */
    AUDIO(0x08),
    /**
     * RTMP video packet (0x09)
     * The client or the server sends this message to send video data to the peer.
     */
    VIDEO(0x09),
    /**
     * RTMP message type 0x0F
     * The client or the server sends this message to send Metadata or any
     * user data to the peer. Metadata includes details about the data (audio, video etc.)
     * like creation time, duration, theme and so on.
     * This is the AMF3-encoded version.
     */
    DATA_AMF3(0x0F),
    /**
     * RTMP message type 0x10
     * A shared object is a Flash object (a collection of name value pairs)
     * that are in synchronization across multiple clients, instances, and
     * so on.
     * This is the AMF3 version: kMsgContainerEx=16 for AMF3.
     */
    SHARED_OBJECT_AMF3(0x10),
    /**
     * RTMP message type 0x11
     * Command messages carry the AMF-encoded commands between the client
     * and the server.
     * A command message consists of command name, transaction ID, and command object that
     * contains related parameters.
     * This is the AMF3-encoded version.
     */
    COMMAND_AMF3(0x11),
    /**
     * RTMP message type 0x12
     * The client or the server sends this message to send Metadata or any
     * user data to the peer. Metadata includes details about the data (audio, video etc.)
     * like creation time, duration, theme and so on.
     * This is the AMF0-encoded version.
     */
    DATA_AMF0(0x12),
    /**
     * RTMP message type 0x14
     * Command messages carry the AMF-encoded commands between the client
     * and the server.
     * A command message consists of command name, transaction ID, and command object that
     * contains related parameters.
     * This is the common AMF0 version, also known as INVOKE in some RTMP implementations.
     */
    COMMAND_AMF0(0x14),
    /**
     * RTMP message type 0x13
     * A shared object is a Flash object (a collection of name value pairs)
     * that are in synchronization across multiple clients, instances, and
     * so on.
     * This is the AMF0 version: kMsgContainer=19 for AMF0.
     */
    SHARED_OBJECT_AMF0(0x13),
    /**
     * RTMP message type 0x16
     * An aggregate message is a single message that contains a list of sub-messages.
     */
    AGGREGATE_MESSAGE(0x16);
    private byte value;
    private static final Map<Byte, MessageType> quickLookupMap = new HashMap<>();

    static {
        for (MessageType messageTypId : MessageType.values()) {
            quickLookupMap.put(messageTypId.getValue(), messageTypId);
        }
    }

    MessageType(int value) {
        this.value = (byte) value;
    }

    /** Returns the value of this chunk type */
    public byte getValue() {
        return value;
    }

    public static MessageType valueOf(byte messageTypeId) {
        if (quickLookupMap.containsKey(messageTypeId)) {
            return quickLookupMap.get(messageTypeId);
        } else {
            throw new IllegalArgumentException("Unknown message type byte: " + Util.toHexString(messageTypeId));
        }
    }
}
