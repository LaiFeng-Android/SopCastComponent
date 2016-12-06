package com.laifeng.sopcastsdk.utils;

import android.util.Log;

/**
 * @Title: SopCastLog
 * @Package com.laifeng.sopcastsdk
 * @Description:
 * @Author Jim
 * @Date 16/6/12
 * @Time 下午3:49
 * @Version
 */
public class SopCastLog {
    private static boolean open = false;

    public static void isOpen(boolean isOpen) {
        open = isOpen;
    }

    public static void d(String tag, String msg) {
        if(open) {
            Log.d(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if(open) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if(open) {
            Log.e(tag, msg);
        }
    }
}
