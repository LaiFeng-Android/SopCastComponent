package com.laifeng.sopcastsdk.video.effect;

import android.content.Context;

import com.laifeng.sopcastsdk.video.GLSLFileUtils;

/**
 * @Title: NullEffect
 * @Package com.laifeng.sopcastsdk.video.effect
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午2:03
 * @Version
 */
public class NullEffect extends Effect{
    private static final String NULL_EFFECT_VERTEX = "null/vertexshader.glsl";
    private static final String NULL_EFFECT_FRAGMENT = "null/fragmentshader.glsl";

    public NullEffect(Context context) {
        super();
        String vertexShader = GLSLFileUtils.getFileContextFromAssets(context, NULL_EFFECT_VERTEX);
        String fragmentShader = GLSLFileUtils.getFileContextFromAssets(context, NULL_EFFECT_FRAGMENT);
        super.setShader(vertexShader, fragmentShader);
    }
}
