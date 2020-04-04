package com.yzq.zxinglibrary.decode;

import com.google.zxing.Result;

/**
 * Created by yzq on 2017/10/18.
 * <p>
 * 解析图片的回调
 */

public interface DecodeImgCallback {
    void onImageDecodeSuccess(Result result);

    void onImageDecodeFailed();
}
