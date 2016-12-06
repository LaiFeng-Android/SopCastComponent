package com.laifeng.sopcastsdk.stream.amf;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Title: AmfDecoder
 * @Package com.jimfengfly.rtmppublisher.amf
 * @Description:
 * @Author Jim
 * @Date 2016/11/28
 * @Time 下午12:51
 * @Version
 */

public class AmfDecoder {
    public static AmfData readFrom(InputStream in) throws IOException {

        byte amfTypeByte = (byte) in.read();
        AmfType amfType = AmfType.valueOf(amfTypeByte);

        AmfData amfData;
        switch (amfType) {
            case NUMBER:
                amfData = new AmfNumber();
                break;
            case BOOLEAN:
                amfData = new AmfBoolean();
                break;
            case STRING:
                amfData = new AmfString();
                break;
            case OBJECT:
                amfData = new AmfObject();
                break;
            case NULL:
                return new AmfNull();
            case UNDEFINED:
                return new AmfUndefined();
            case MAP:
                amfData = new AmfMap();
                break;
            case ARRAY:
                amfData = new AmfArray();
                break;
            default:
                throw new IOException("Unknown/unimplemented AMF data type: " + amfType);
        }

        amfData.readFrom(in);
        return amfData;
    }
}
