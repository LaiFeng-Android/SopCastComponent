package com.laifeng.sopcastsdk.stream.sender.rtmp.packets;

import com.laifeng.sopcastsdk.stream.sender.rtmp.Util;
import com.laifeng.sopcastsdk.stream.sender.rtmp.io.SessionInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A "Abort" RTMP control message, received on chunk stream ID 2 (control channel)
 * 
 * @author francois
 */
public class Abort extends Chunk {

    private int chunkStreamId;
    
    public Abort(ChunkHeader header) {
        super(header);
    }

    public Abort(int chunkStreamId) {
        super(new ChunkHeader(ChunkType.TYPE_0_FULL, SessionInfo.RTMP_CONTROL_CHANNEL, MessageType.SET_CHUNK_SIZE));
        this.chunkStreamId = chunkStreamId;
    }

    /** @return the ID of the chunk stream to be aborted */
    public int getChunkStreamId() {
        return chunkStreamId;
    }

    /** Sets the ID of the chunk stream to be aborted */
    public void setChunkStreamId(int chunkStreamId) {
        this.chunkStreamId = chunkStreamId;
    }

    @Override
    public void readBody(InputStream in) throws IOException {
        // Value is received in the 4 bytes of the body
        chunkStreamId = Util.readUnsignedInt32(in);
    }

    @Override
    protected void writeBody(OutputStream out) throws IOException {
        Util.writeUnsignedInt32(out, chunkStreamId);
    }
}
