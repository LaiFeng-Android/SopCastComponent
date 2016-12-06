package com.laifeng.sopcastsdk.stream.amf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @Title: AmfNull
 * @Package com.jimfengfly.rtmppublisher.amf
 * @Description:
 * @Author Jim
 * @Date 2016/11/28
 * @Time 下午12:51
 * @Version
 */

public class AmfNull implements AmfData {
    public static final int SIZE = 1;

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(AmfType.NULL.getValue());
    }

    @Override
    public void readFrom(InputStream in) throws IOException {
        // Skip data type byte (we assume it's already read)
    }

    public static void writeNullTo(OutputStream out) throws IOException {
        out.write(AmfType.NULL.getValue());
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public byte[] getBytes() {
        byte[] data = new byte[1];
        data[0] = AmfType.NULL.getValue();
        return data;
    }
}
