/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laifeng.sopcastsdk.stream.sender.rtmp.packets;

import android.util.Log;

import com.laifeng.sopcastsdk.stream.sender.rtmp.Util;
import com.laifeng.sopcastsdk.stream.sender.rtmp.io.SessionInfo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author francois, leoma
 */
public class ChunkHeader {

    private static final String TAG = "ChunkHeader";
    private ChunkType chunkType;
    private int chunkStreamId;
    private int absoluteTimestamp;
    private int timestampDelta = -1;
    private int packetLength;
    private MessageType messageType;
    private int messageStreamId;
    private int extendedTimestamp;

    public ChunkHeader() {
    }

    public ChunkHeader(ChunkType chunkType, int chunkStreamId, MessageType messageType) {
        this.chunkType = chunkType;
        this.chunkStreamId = chunkStreamId;
        this.messageType = messageType;
    }

    public static ChunkHeader readHeader(InputStream in, SessionInfo sessionInfo) throws IOException {
        ChunkHeader rtmpHeader = new ChunkHeader();
        rtmpHeader.readHeaderImpl(in, sessionInfo);
        return rtmpHeader;
    }

    private void readHeaderImpl(InputStream in, SessionInfo sessionInfo) throws IOException {

        int basicHeaderByte = in.read();
        if (basicHeaderByte == -1) {
            throw new EOFException("Unexpected EOF while reading RTMP packet basic header");
        }
        // Read byte 0: chunk type and chunk stream ID
        parseBasicHeader((byte) basicHeaderByte);

        switch (chunkType) {
            case TYPE_0_FULL: { //  b00 = 12 byte header (full header) 
                // Read bytes 1-3: Absolute timestamp
                absoluteTimestamp = Util.readUnsignedInt24(in);
                timestampDelta = 0;
                // Read bytes 4-6: Packet length
                packetLength = Util.readUnsignedInt24(in);
                // Read byte 7: Message type ID
                messageType = MessageType.valueOf((byte) in.read());
                // Read bytes 8-11: Message stream ID (apparently little-endian order)
                byte[] messageStreamIdBytes = new byte[4];
                Util.readBytesUntilFull(in, messageStreamIdBytes);
                messageStreamId = Util.toUnsignedInt32LittleEndian(messageStreamIdBytes);
                // Read bytes 1-4: Extended timestamp
                extendedTimestamp = absoluteTimestamp >= 0xffffff ? Util.readUnsignedInt32(in) : 0;
                if (extendedTimestamp != 0) {
                    absoluteTimestamp = extendedTimestamp;
                }
                sessionInfo.putPreReceiveChunkHeader(chunkStreamId, this);
                break;
            }
            case TYPE_1_LARGE: { // b01 = 8 bytes - like type 0. not including message stream ID (4 last bytes)
                // Read bytes 1-3: Timestamp delta
                timestampDelta = Util.readUnsignedInt24(in);
                // Read bytes 4-6: Packet length
                packetLength = Util.readUnsignedInt24(in);
                // Read byte 7: Message type ID
                messageType = MessageType.valueOf((byte) in.read());
                // Read bytes 1-4: Extended timestamp delta
                extendedTimestamp = timestampDelta >= 0xffffff ? Util.readUnsignedInt32(in) : 0;
                ChunkHeader prevHeader = sessionInfo.getPreReceiveChunkHeader(chunkStreamId);
                if (prevHeader != null) {
                    messageStreamId = prevHeader.messageStreamId;
                    absoluteTimestamp = extendedTimestamp != 0 ? extendedTimestamp : prevHeader.absoluteTimestamp + timestampDelta;
                } else {
                    messageStreamId = 0;
                    absoluteTimestamp = extendedTimestamp != 0 ? extendedTimestamp : timestampDelta;
                }
                sessionInfo.putPreReceiveChunkHeader(chunkStreamId, this);
                break;
            }
            case TYPE_2_TIMESTAMP_ONLY: { // b10 = 4 bytes - Basic Header and timestamp (3 bytes) are included
                // Read bytes 1-3: Timestamp delta
                timestampDelta = Util.readUnsignedInt24(in);
                // Read bytes 1-4: Extended timestamp delta
                extendedTimestamp = timestampDelta >= 0xffffff ? Util.readUnsignedInt32(in) : 0;
                ChunkHeader prevHeader = sessionInfo.getPreReceiveChunkHeader(chunkStreamId);
                packetLength = prevHeader.packetLength;
                messageType = prevHeader.messageType;
                messageStreamId = prevHeader.messageStreamId;
                absoluteTimestamp = extendedTimestamp != 0 ? extendedTimestamp : prevHeader.absoluteTimestamp + timestampDelta;
                sessionInfo.putPreReceiveChunkHeader(chunkStreamId, this);
                break;
            }
            case TYPE_3_NO_BYTE: { // b11 = 1 byte: basic header only
                ChunkHeader prevHeader = sessionInfo.getPreReceiveChunkHeader(chunkStreamId);
                // Read bytes 1-4: Extended timestamp
                extendedTimestamp = prevHeader.timestampDelta >= 0xffffff ? Util.readUnsignedInt32(in) : 0;
                timestampDelta = extendedTimestamp != 0 ? 0xffffff : prevHeader.timestampDelta;
                packetLength = prevHeader.packetLength;
                messageType = prevHeader.messageType;
                messageStreamId = prevHeader.messageStreamId;
                absoluteTimestamp = extendedTimestamp != 0 ? extendedTimestamp : prevHeader.absoluteTimestamp + timestampDelta;
                sessionInfo.putPreReceiveChunkHeader(chunkStreamId, this);
                break;
            }
            default:
                Log.e(TAG, "readHeaderImpl(): Invalid chunk type; basic header byte was: " + Util.toHexString((byte) basicHeaderByte));
                throw new IOException("Invalid chunk type; basic header byte was: " + Util.toHexString((byte) basicHeaderByte));
        }
    }

    public void writeTo(OutputStream out, ChunkType chunkType, final SessionInfo sessionInfo) throws IOException {
        // Write basic header byte
        out.write(((byte) (chunkType.getValue() << 6) | chunkStreamId));
        switch (chunkType) {
            case TYPE_0_FULL: { //  b00 = 12 byte header (full header)
                absoluteTimestamp = (int) sessionInfo.markAbsoluteTimestampTx();
                Util.writeUnsignedInt24(out, absoluteTimestamp >= 0xffffff ? 0xffffff : absoluteTimestamp);
                Util.writeUnsignedInt24(out, packetLength);
                out.write(messageType.getValue());
                Util.writeUnsignedInt32LittleEndian(out, messageStreamId);
                if (absoluteTimestamp >= 0xffffff) {
                    extendedTimestamp = absoluteTimestamp;
                    Util.writeUnsignedInt32(out, extendedTimestamp);
                }
                break;
            }
            case TYPE_1_LARGE: { // b01 = 8 bytes - like type 0. not including message ID (4 last bytes)
                absoluteTimestamp = (int) sessionInfo.markAbsoluteTimestampTx();
                ChunkHeader preChunkHeader = sessionInfo.getPreSendChunkHeader(chunkStreamId);
                timestampDelta = absoluteTimestamp - preChunkHeader.absoluteTimestamp;
                Util.writeUnsignedInt24(out, absoluteTimestamp >= 0xffffff ? 0xffffff : timestampDelta);
                Util.writeUnsignedInt24(out, packetLength);
                out.write(messageType.getValue());
                if (absoluteTimestamp >= 0xffffff) {
                    extendedTimestamp = absoluteTimestamp;
                    Util.writeUnsignedInt32(out, absoluteTimestamp);
                }
                break;
            }
            case TYPE_2_TIMESTAMP_ONLY: { // b10 = 4 bytes - Basic Header and timestamp (3 bytes) are included
                absoluteTimestamp = (int) sessionInfo.markAbsoluteTimestampTx();
                ChunkHeader preChunkHeader = sessionInfo.getPreSendChunkHeader(chunkStreamId);
                timestampDelta = absoluteTimestamp - preChunkHeader.absoluteTimestamp;
                Util.writeUnsignedInt24(out, (absoluteTimestamp >= 0xffffff) ? 0xffffff : timestampDelta);
                if (absoluteTimestamp >= 0xffffff) {
                    extendedTimestamp = absoluteTimestamp;
                    Util.writeUnsignedInt32(out, extendedTimestamp);
                }
                break;
            }
            case TYPE_3_NO_BYTE: { // b11 = 1 byte: basic header only
                absoluteTimestamp = (int) sessionInfo.markAbsoluteTimestampTx();
                if (absoluteTimestamp >= 0xffffff) {
                    extendedTimestamp = absoluteTimestamp;
                    Util.writeUnsignedInt32(out, extendedTimestamp);
                }
                break;
            }
            default:
                throw new IOException("Invalid chunk type: " + chunkType);
        }
    }

    private void parseBasicHeader(byte basicHeaderByte) {
        chunkType = ChunkType.valueOf((byte) ((0xff & basicHeaderByte) >>> 6)); // 2 most significant bits define the chunk type
        chunkStreamId = basicHeaderByte & 0x3F; // 6 least significant bits define chunk stream ID
    }

    /** @return the RTMP chunk stream ID (channel ID) for this chunk */
    public int getChunkStreamId() {
        return chunkStreamId;
    }

    public int getPacketLength() {
        return packetLength;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    /** Sets the RTMP chunk stream ID (channel ID) for this chunk */
    public void setChunkStreamId(int channelId) {
        this.chunkStreamId = channelId;
    }

    public void setMessageStreamId(int messageStreamId) {
        this.messageStreamId = messageStreamId;
    }

    public void setPacketLength(int packetLength) {
        this.packetLength = packetLength;
    }

    @Override
    public String toString() {
        return "ChunkType:" + chunkType + " ChunkStreamId:" + chunkStreamId + " absoluteTimestamp:" + absoluteTimestamp + " timestampDelta:" + timestampDelta +
                " messageLength:" + packetLength + " messageType:" + messageType +
                " messageStreamId:" + messageStreamId + " extendedTimestamp:" + extendedTimestamp;
    }
}
