package com.laifeng.sopcastsdk.constant;

/**
 * @Title: SopCastConstant
 * @Package com.laifeng.sopcastsdk
 * @Description:
 * @Author Jim
 * @Date 16/9/14
 * @Time 下午2:53
 * @Version
 */
public class SopCastConstant {
    public static final String TAG = "SopCast";
    public static final String VERSION = "1.0";
    public static final String BRANCH = "open-source";

    public static final String SHARDE_NULL_VERTEX = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "uniform   mat4 uPosMtx;\n" +
            "uniform   mat4 uTexMtx;\n" +
            "varying   vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "  gl_Position = uPosMtx * position;\n" +
            "  textureCoordinate   = (uTexMtx * inputTextureCoordinate).xy;\n" +
            "}";

    public static final String SHARDE_NULL_FRAGMENT = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    vec4 tc = texture2D(sTexture, textureCoordinate);\n" +
            "    gl_FragColor = vec4(tc.r, tc.g, tc.b, 1.0);\n" +
            "}";
}
