attribute vec4 position;
attribute vec4 inputTextureCoordinate;

uniform   mat4 uPosMtx;
uniform   mat4 uTexMtx;
varying   vec2 textureCoordinate;
void main() {
  gl_Position = uPosMtx * position;
  textureCoordinate   = (uTexMtx * inputTextureCoordinate).xy;
}