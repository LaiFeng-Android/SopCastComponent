package com.yzq.zxinglibrary.encode;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

public class CodeCreator {


    /*生成二维码*/
    public static Bitmap createQRCode(String content, int w, int h, Bitmap logo) {


        if (TextUtils.isEmpty(content)) {
            return null;
        }
        /*偏移量*/
        int offsetX = w / 2;
        int offsetY = h / 2;

        /*生成logo*/
        Bitmap logoBitmap = null;

        if (logo != null) {
            Matrix matrix = new Matrix();
            float scaleFactor = Math.min(w * 1.0f / 5 / logo.getWidth(), h * 1.0f / 5 / logo.getHeight());
            matrix.postScale(scaleFactor, scaleFactor);
            logoBitmap = Bitmap.createBitmap(logo, 0, 0, logo.getWidth(), logo.getHeight(), matrix, true);
        }


        /*如果log不为null,重新计算偏移量*/
        int logoW = 0;
        int logoH = 0;
        if (logoBitmap != null) {
            logoW = logoBitmap.getWidth();
            logoH = logoBitmap.getHeight();
            offsetX = (w - logoW) / 2;
            offsetY = (h - logoH) / 2;
        }

        /*指定为UTF-8*/
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        //容错级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //设置空白边距的宽度
        hints.put(EncodeHintType.MARGIN, 0);
        // 生成二维矩阵,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
        BitMatrix matrix = null;
        try {
            matrix = new MultiFormatWriter().encode(content,
                    BarcodeFormat.QR_CODE, w, h, hints);

            // 二维矩阵转为一维像素数组,也就是一直横着排了
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (x >= offsetX && x < offsetX + logoW && y >= offsetY && y < offsetY + logoH) {
                        int pixel = logoBitmap.getPixel(x - offsetX, y - offsetY);
                        if (pixel == 0) {
                            if (matrix.get(x, y)) {
                                pixel = 0xff000000;
                            } else {
                                pixel = 0xffffffff;
                            }
                        }
                        pixels[y * w + x] = pixel;
                    } else {
                        if (matrix.get(x, y)) {
                            pixels[y * w + x] = 0xff000000;
                        } else {
                            pixels[y * w + x] = 0xffffffff;
                        }
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(w, h,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
            return bitmap;


        } catch (WriterException e) {

            System.out.print(e);
            return null;
        }


    }

}
