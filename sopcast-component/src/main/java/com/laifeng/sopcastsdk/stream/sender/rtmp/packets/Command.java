package com.laifeng.sopcastsdk.stream.sender.rtmp.packets;

import com.laifeng.sopcastsdk.stream.amf.AmfNumber;
import com.laifeng.sopcastsdk.stream.amf.AmfString;
import com.laifeng.sopcastsdk.stream.sender.rtmp.io.SessionInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Encapsulates an command/"invoke" RTMP packet
 * 
 * Invoke/command packet structure (AMF encoded):
 * (String) <commmand name>
 * (Number) <Transaction ID>
 * (Mixed) <Argument> ex. Null, String, Object: {key1:value1, key2:value2 ... }
 * 
 * @author francois
 */
public class Command extends VariableBodyRtmpPacket {

    private static final String TAG = "Command";

    private String commandName;
    private int transactionId;    

    public Command(ChunkHeader header) {
        super(header);
    }
    
    public Command(String commandName, int transactionId) {
        super(new ChunkHeader(ChunkType.TYPE_0_FULL, SessionInfo.RTMP_COMMAND_CHANNEL, MessageType.COMMAND_AMF0));
        this.commandName = commandName;
        this.transactionId = transactionId;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }    

    @Override
    public void readBody(InputStream in) throws IOException {
        // The command name and transaction ID are always present (AMF string followed by number)
        commandName = AmfString.readStringFrom(in, false);
        transactionId = (int) AmfNumber.readNumberFrom(in);
        int bytesRead = AmfString.sizeOf(commandName, false) + AmfNumber.SIZE;
        readVariableData(in, bytesRead);
    }

    @Override
    protected void writeBody(OutputStream out) throws IOException {
        AmfString.writeStringTo(out, commandName, false);
        AmfNumber.writeNumberTo(out, transactionId);
        // Write body data
        writeVariableData(out);
    }
    
    @Override
    public String toString() {
        return "RTMP Command (command: " + commandName + ", transaction ID: " + transactionId + ")";
    }
}
