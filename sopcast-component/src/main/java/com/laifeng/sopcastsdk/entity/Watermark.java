package com.laifeng.sopcastsdk.entity;

import android.graphics.Bitmap;

/**
 * @Title: Watermark
 * @Package com.laifeng.sopcastsdk.video
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午2:32
 * @Version
 */
public class Watermark {
    public Bitmap markImg;
    public int width;
    public int height;
    public int orientation;
    public int vMargin;
    public int hMargin;

    public Watermark(Bitmap img, int width, int height, int orientation, int vmargin, int hmargin) {
        markImg = img;
        this.width = width;
        this.height = height;
        this.orientation = orientation;
        vMargin = vmargin;
        hMargin = hmargin;
    }
}
