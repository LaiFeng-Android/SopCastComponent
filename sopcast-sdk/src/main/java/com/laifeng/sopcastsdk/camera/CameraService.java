package com.laifeng.sopcastsdk.camera;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CameraService extends Service {

    protected Class<?> cls;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
