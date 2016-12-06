package com.laifeng.sopcastsdk.stream.sender.rtmp.io;

import android.util.Log;

import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Abort;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Acknowledgement;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Audio;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Command;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Data;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.ChunkHeader;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Chunk;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.SetChunkSize;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.SetPeerBandwidth;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.UserControl;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Video;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.WindowAckSize;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 *
 * @author francois
 */
public class RtmpDecoder {

    private static final String TAG = "RtmpDecoder";

    private SessionInfo sessionInfo;

    private HashMap<Integer, StoreChunk> storeChunkHashMap = new HashMap<>();

    public RtmpDecoder(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public Chunk readPacket(InputStream in) throws IOException {

        ChunkHeader header = ChunkHeader.readHeader(in, sessionInfo);
        Chunk rtmpPacket;
        Log.d(TAG, "readPacket(): header.messageType: " + header.getMessageType());

        int messageLength = header.getPacketLength();
        if (header.getPacketLength() > sessionInfo.getRxChunkSize()) {
            //Log.d(TAG, "readPacket(): packet size (" + header.getPacketLength() + ") is bigger than chunk size (" + rtmpSessionInfo.getChunkSize() + "); storing chunk data");
            // This packet consists of more than one chunk; store the chunks in the chunk stream until everything is read
            StoreChunk storeChunk = storeChunkHashMap.get(header.getChunkStreamId());
            if(storeChunk == null) {
                storeChunk = new StoreChunk();
                storeChunkHashMap.put(header.getChunkStreamId(), storeChunk);
            }
            if (!storeChunk.storeChunk(in, messageLength, sessionInfo.getRxChunkSize())) {
                Log.d(TAG, " readPacket(): returning null because of incomplete packet");
                return null; // packet is not yet complete
            } else {
                Log.d(TAG, " readPacket(): stored chunks complete packet; reading packet");
                in = storeChunk.getStoredInputStream();
            }
        } else {
            //Log.d(TAG, "readPacket(): packet size (" + header.getPacketLength() + ") is LESS than chunk size (" + rtmpSessionInfo.getChunkSize() + "); reading packet fully");
        }

        switch (header.getMessageType()) {

            case SET_CHUNK_SIZE:
                SetChunkSize setChunkSize = new SetChunkSize(header);
                setChunkSize.readBody(in);
                Log.d(TAG, "readPacket(): Setting chunk size to: " + setChunkSize.getChunkSize());
                sessionInfo.setRxChunkSize(setChunkSize.getChunkSize());
                return null;
            case ABORT:
                rtmpPacket = new Abort(header);
                break;
            case USER_CONTROL_MESSAGE:
                rtmpPacket = new UserControl(header);
                break;
            case WINDOW_ACKNOWLEDGEMENT_SIZE:
                rtmpPacket = new WindowAckSize(header);
                break;
            case SET_PEER_BANDWIDTH:
                rtmpPacket = new SetPeerBandwidth(header);
                break;
            case AUDIO:
                rtmpPacket = new Audio(header);
                break;
            case VIDEO:
                rtmpPacket = new Video(header);
                break;
            case COMMAND_AMF0:
                rtmpPacket = new Command(header);
                break;
            case DATA_AMF0:
                rtmpPacket = new Data(header);
                break;
            case ACKNOWLEDGEMENT:
                rtmpPacket = new Acknowledgement(header);
                break;
            default:
                throw new IOException("No packet body implementation for message type: " + header.getMessageType());
        }                
        rtmpPacket.readBody(in);
        return rtmpPacket;
    }

    public void clearStoredChunks(int chunkStreamId) {
        StoreChunk storeChunk = storeChunkHashMap.get(chunkStreamId);
        if(storeChunk != null) {
            storeChunk.clearStoredChunks();
        }
    }
}
