package com.laifeng.sopcastsdk.stream.amf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @Title: AmfString
 * @Package com.jimfengfly.rtmppublisher.amf
 * @Description:
 * @Author Jim
 * @Date 2016/11/28
 * @Time 下午2:08
 * @Version
 */

public class AmfString implements AmfData {
    private static final String TAG = "AmfString";

    private String value;
    private boolean key;
    private int size = -1;

    public AmfString() {
    }

    public AmfString(String value, boolean isKey) {
        this.value = value;
        this.key = isKey;
    }

    public AmfString(String value) {
        this(value, false);
    }

    public AmfString(boolean isKey) {
        this.key = isKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isKey() {
        return key;
    }

    public void setKey(boolean key) {
        this.key = key;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        // Strings are ASCII encoded
        byte[] byteValue = this.value.getBytes();
        // Write the STRING data type definition (except if this String is used as a key)
        if (!key) {
            out.write(AmfType.STRING.getValue());
        }
        // Write 2 bytes indicating string length
        Util.writeUnsignedInt16(out, byteValue.length);
        // Write string
        out.write(byteValue);
    }

    @Override
    public void readFrom(InputStream in) throws IOException {
        // Skip data type byte (we assume it's already read)
        int length = Util.readUnsignedInt16(in);
        size = 3 + length; // 1 + 2 + length
        // Read string value
        byte[] byteValue = new byte[length];
        Util.readBytesUntilFull(in, byteValue);
        value = new String(byteValue);
    }

    public static String readStringFrom(InputStream in, boolean isKey) throws IOException {
        if (!isKey) {
            // Read past the data type byte
            in.read();
        }
        int length = Util.readUnsignedInt16(in);
        // Read string value
        byte[] byteValue = new byte[length];
        Util.readBytesUntilFull(in, byteValue);
        return new String(byteValue);
    }

    public static void writeStringTo(OutputStream out, String string, boolean isKey) throws IOException {
        // Strings are ASCII encoded
        byte[] byteValue = string.getBytes();
        // Write the STRING data type definition (except if this String is used as a key)
        if (!isKey) {
            out.write(AmfType.STRING.getValue());
        }
        // Write 2 bytes indicating string length
        Util.writeUnsignedInt16(out, byteValue.length);
        // Write string
        out.write(byteValue);
    }

    @Override
    public int getSize() {
        size = (isKey() ? 0 : 1) + 2 + value.getBytes().length;
        return size;
    }

    @Override
    public byte[] getBytes() {
        int size = getSize();
        ByteBuffer dataBuffer = ByteBuffer.allocate(size);
        if(!isKey()) {
            dataBuffer.put(AmfType.STRING.getValue());
        }
        dataBuffer.putShort((short) value.getBytes().length);
        dataBuffer.put(value.getBytes());
        return dataBuffer.array();
    }

    /** @return the byte size of the resulting AMF string of the specified value */
    public static int sizeOf(String string, boolean isKey) {
        int size = (isKey ? 0 : 1) + 2 + string.getBytes().length;
        return size;
    }
}
