package com.laifeng.sopcastsdk.camera;

/**
 * @Title: CameraData
 * @Package com.youku.crazytogether.app.modules.livehouse_new.widget.sopCast
 * @Description:
 * @Author Jim
 * @Date 16/3/12
 * @Time 上午10:50
 * @Version
 */
public class CameraData {

    public static final int FACING_FRONT = 1;
    public static final int FACING_BACK = 2;

    public int cameraID;            //camera的id
    public int cameraFacing;        //区分前后摄像头
    public int cameraWidth;         //camera的宽度
    public int cameraHeight;        //camera的高度
    public boolean hasLight;
    public int orientation;
    public boolean supportTouchFocus;
    public boolean touchFocusMode;

    public CameraData(int id, int facing, int width, int height){
        cameraID = id;
        cameraFacing = facing;
        cameraWidth = width;
        cameraHeight = height;
    }

    public CameraData(int id, int facing) {
        cameraID = id;
        cameraFacing = facing;
    }
}
