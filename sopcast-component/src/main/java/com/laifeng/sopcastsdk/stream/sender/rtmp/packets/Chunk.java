package com.laifeng.sopcastsdk.stream.sender.rtmp.packets;

import com.laifeng.sopcastsdk.stream.sender.rtmp.io.SessionInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author francois, leo
 */
public abstract class Chunk {
     
    protected ChunkHeader header;

    public Chunk(ChunkHeader header) {
        this.header = header;
    }

    public ChunkHeader getChunkHeader() {
        return header;
    }
    
    public abstract void readBody(InputStream in) throws IOException;    
    
    protected abstract void writeBody(OutputStream out) throws IOException;
           
    public void writeTo(OutputStream out, final SessionInfo sessionInfo) throws IOException {
        int chunkSize = sessionInfo.getTxChunkSize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeBody(baos);        
        byte[] body = baos.toByteArray();        
        header.setPacketLength(body.length);
        // Write header for first chunk
        header.writeTo(out, ChunkType.TYPE_0_FULL, sessionInfo);
        int remainingBytes = body.length;
        int pos = 0;
        while (remainingBytes > chunkSize) {
            // Write packet for chunk
            out.write(body, pos, chunkSize);
            remainingBytes -= chunkSize;
            pos += chunkSize;
            // Write header for remain chunk
            header.writeTo(out, ChunkType.TYPE_3_NO_BYTE, sessionInfo);
        }
        out.write(body, pos, remainingBytes);
    }
}
