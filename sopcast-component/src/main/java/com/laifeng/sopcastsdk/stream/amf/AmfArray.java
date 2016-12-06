package com.laifeng.sopcastsdk.stream.amf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @Title: AmfArray
 * @Package com.jimfengfly.rtmppublisher.amf
 * @Description:
 * @Author Jim
 * @Date 2016/11/28
 * @Time 下午2:51
 * @Version
 */

public class AmfArray implements AmfData {

    private List<AmfData> items;
    private int size = -1;

    @Override
    public void writeTo(OutputStream out) throws IOException {
        // Begin the map/object/array/whatever exactly this is
        out.write(AmfType.ARRAY.getValue());
        // Write the "array size"
        Util.writeUnsignedInt32(out, items.size());
        for (AmfData item : items) {
            item.writeTo(out);
        }
    }

    @Override
    public void readFrom(InputStream in) throws IOException {
        // Skip data type byte (we assume it's already read)
        int length = Util.readUnsignedInt32(in);
        size = 5; // 1 + 4
        items = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            AmfData dataItem = AmfDecoder.readFrom(in);
            size += dataItem.getSize();
            items.add(dataItem);
        }
    }

    @Override
    public int getSize() {
        if (size == -1) {
            size = 5; // 1 + 4
            if (items != null) {
                for (AmfData dataItem : items) {
                    size += dataItem.getSize();
                }
            }
        }
        return size;
    }

    @Override
    public byte[] getBytes() {
        int size = getSize();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.put(AmfType.ARRAY.getValue());
        byteBuffer.putInt(items.size());
        for (AmfData item : items) {
            byteBuffer.put(item.getBytes());
        }
        return byteBuffer.array();
    }

    /** @return the amount of items in this the array */
    public int getLength() {
        return items != null ? items.size() : 0;
    }

    public List<AmfData> getItems() {
        if (items == null) {
            items = new ArrayList<AmfData>();
        }
        return items;
    }

    public void addItem(AmfData dataItem) {
        getItems().add(this);
    }
}
