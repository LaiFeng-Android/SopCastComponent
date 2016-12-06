#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 textureCoordinate;
uniform samplerExternalOES sTexture;
void main() {
    vec4 tc = texture2D(sTexture, textureCoordinate);
    gl_FragColor = vec4(tc.r, tc.g, tc.b, 1.0);
}