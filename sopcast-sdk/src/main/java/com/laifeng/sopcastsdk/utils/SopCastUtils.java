package com.laifeng.sopcastsdk.utils;

import android.os.Build;
import android.os.Looper;

/**
 * @Title: SopCastUtils
 * @Package com.laifeng.sopcastsdk.utils
 * @Description:
 * @Author Jim
 * @Date 2016/11/2
 * @Time 下午1:14
 * @Version
 */

public class SopCastUtils {

    public interface INotUIProcessor {
        void process();
    }

    public static void processNotUI(final INotUIProcessor processor) {
        if(Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    processor.process();
                }
            }).start();
        } else {
            processor.process();
        }
    }

    public static boolean isOverLOLLIPOP() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
