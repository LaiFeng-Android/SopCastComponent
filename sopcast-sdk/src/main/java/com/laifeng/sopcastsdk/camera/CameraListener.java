package com.laifeng.sopcastsdk.camera;

/**
 * @Title: CameraListener
 * @Package com.laifeng.sopcastsdk.camera
 * @Description:
 * @Author Jim
 * @Date 16/7/18
 * @Time 上午10:42
 * @Version
 */
public interface CameraListener {
    int CAMERA_NOT_SUPPORT = 1;
    int NO_CAMERA = 2;
    int CAMERA_DISABLED = 3;
    int CAMERA_OPEN_FAILED = 4;

    void onOpenSuccess();
    void onOpenFail(int error);
    void onCameraChange();
}
