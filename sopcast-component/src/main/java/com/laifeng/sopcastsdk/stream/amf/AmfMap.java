package com.laifeng.sopcastsdk.stream.amf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @Title: AmfMap
 * @Package com.jimfengfly.rtmppublisher.amf
 * @Description:
 * @Author Jim
 * @Date 2016/11/28
 * @Time 下午2:48
 * @Version
 */

public class AmfMap extends AmfObject {
    @Override
    public void writeTo(OutputStream out) throws IOException {
        // Begin the map/object/array/whatever exactly this is
        out.write(AmfType.MAP.getValue());

        // Write the "array size"
        Util.writeUnsignedInt32(out, properties.size());

        // Write key/value pairs in this object
        for (Map.Entry<String, AmfData> entry : properties.entrySet()) {
            // The key must be a STRING type, and thus the "type-definition" byte is implied (not included in message)
            AmfString.writeStringTo(out, entry.getKey(), true);
            entry.getValue().writeTo(out);
        }

        // End the object
        out.write(OBJECT_END_MARKER);
    }

    @Override
    public void readFrom(InputStream in) throws IOException {
        // Skip data type byte (we assume it's already read)
        int length = Util.readUnsignedInt32(in); // Seems this is always 0
        super.readFrom(in);
        size += 4; // Add the bytes read for parsing the array size (length)
    }

    @Override
    public int getSize() {
        if (size == -1) {
            size = super.getSize();
            size += 4; // array length bytes
        }
        return size;
    }

    @Override
    public byte[] getBytes() {
        int size = getSize();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.put(AmfType.OBJECT.getValue());
        byteBuffer.putInt(properties.size());
        // Write key/value pairs in this object
        for (Map.Entry<String, AmfData> entry : properties.entrySet()) {
            // The key must be a STRING type, and thus the "type-definition" byte is implied (not included in message)
            AmfString keyString = new AmfString(entry.getKey(), true);
            byteBuffer.put(keyString.getBytes());
            byteBuffer.put(entry.getValue().getBytes());
        }
        byteBuffer.put(OBJECT_END_MARKER);
        return byteBuffer.array();
    }
}
