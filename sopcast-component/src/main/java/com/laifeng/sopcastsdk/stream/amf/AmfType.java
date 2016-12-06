package com.laifeng.sopcastsdk.stream.amf;

import java.util.HashMap;
import java.util.Map;

/**
 * @Title: AmfType
 * @Package com.jimfengfly.rtmppublisher.amf
 * @Description:
 * @Author Jim
 * @Date 2016/11/28
 * @Time 上午11:13
 * @Version
 */

public enum AmfType {
    /** Number (encoded as IEEE 64-bit double precision floating point number) */
    NUMBER(0x00),
    /** Boolean (Encoded as a single byte of value 0x00 or 0x01) */
    BOOLEAN(0x01),
    /** String (ASCII encoded) */
    STRING(0x02),
    /** Object - set of key/value pairs */
    OBJECT(0x03),
    NULL(0x05),
    UNDEFINED(0x06),
    MAP(0x08),
    ARRAY(0x0A);
    private byte value;
    private static final Map<Byte, AmfType> quickLookupMap = new HashMap<Byte, AmfType>();

    static {
        for (AmfType amfType : AmfType.values()) {
            quickLookupMap.put(amfType.getValue(), amfType);
        }
    }

    private AmfType(int intValue) {
        this.value = (byte) intValue;
    }

    public byte getValue() {
        return value;
    }

    public static AmfType valueOf(byte amfTypeByte) {
        return quickLookupMap.get(amfTypeByte);
    }
}
