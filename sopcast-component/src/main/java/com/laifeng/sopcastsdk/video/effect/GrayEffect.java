package com.laifeng.sopcastsdk.video.effect;

import android.content.Context;

import com.laifeng.sopcastsdk.video.GLSLFileUtils;

/**
 * @Title: GrayEffect
 * @Package com.laifeng.sopcastsdk.video.effect
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午2:11
 * @Version
 */
public class GrayEffect extends Effect{

    private static final String GRAY_EFFECT_VERTEX = "gray/vertexshader.glsl";
    private static final String GRAY_EFFECT_FRAGMENT = "gray/fragmentshader.glsl";

    public GrayEffect(Context context) {
        super();
        String vertexShader = GLSLFileUtils.getFileContextFromAssets(context, GRAY_EFFECT_VERTEX);
        String fragmentShader = GLSLFileUtils.getFileContextFromAssets(context, GRAY_EFFECT_FRAGMENT);
        super.setShader(vertexShader, fragmentShader);
    }
}
