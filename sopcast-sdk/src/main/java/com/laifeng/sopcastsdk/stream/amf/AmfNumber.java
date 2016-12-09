package com.laifeng.sopcastsdk.stream.amf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @Title: AmfNumber
 * @Package com.jimfengfly.rtmppublisher.amf
 * @Description:
 * @Author Jim
 * @Date 2016/11/28
 * @Time 上午11:46
 * @Version
 */

public class AmfNumber implements AmfData{
    double value;
    /** Size of an AMF number, in bytes (including type bit) */
    public static final int SIZE = 9;

    public AmfNumber(double value) {
        this.value = value;
    }

    public AmfNumber() {
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(AmfType.NUMBER.getValue());
        Util.writeDouble(out, value);
    }

    @Override
    public void readFrom(InputStream in) throws IOException {
        // Skip data type byte (we assume it's already read)
        value = Util.readDouble(in);
    }

    public static double readNumberFrom(InputStream in) throws IOException {
        // Skip data type byte
        in.read();
        return Util.readDouble(in);
    }

    public static void writeNumberTo(OutputStream out, double number) throws IOException {
        out.write(AmfType.NUMBER.getValue());
        Util.writeDouble(out, number);
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer dataBuffer = ByteBuffer.allocate(SIZE);
        dataBuffer.put(AmfType.NUMBER.getValue());
        dataBuffer.putDouble(value);
        return dataBuffer.array();
    }
}
