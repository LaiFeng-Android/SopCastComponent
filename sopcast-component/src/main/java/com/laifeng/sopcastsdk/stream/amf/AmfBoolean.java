package com.laifeng.sopcastsdk.stream.amf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @Title: AmfBoolean
 * @Package com.jimfengfly.rtmppublisher.amf
 * @Description:
 * @Author Jim
 * @Date 2016/11/28
 * @Time 上午11:39
 * @Version
 */

public class AmfBoolean implements AmfData{
    public static final int SIZE = 2;

    private boolean value;

    public boolean isValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public AmfBoolean(boolean value) {
        this.value = value;
    }

    public AmfBoolean() {}

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(AmfType.BOOLEAN.getValue());
        out.write(value ? 0x01 : 0x00);
    }

    @Override
    public void readFrom(InputStream in) throws IOException {
        // Skip data type byte (we assume it's already read)
        value = (in.read() == 0x01) ? true : false;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public byte[] getBytes() {
        byte[] data = new byte[2];
        data[0] = AmfType.BOOLEAN.getValue();
        data[1] = (byte) (value ? 0x01 : 0x00);
        return data;
    }
}
