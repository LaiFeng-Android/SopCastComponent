package com.drill.liveDemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.laifeng.sopcastsdk.camera.CameraService;

public class BackgroundService extends CameraService {
    HandlerThread handlerThread = new HandlerThread("cameraHolder");

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cls = LandscapeActivity.class;
        startForeground();
    }

    public void startForeground() {
        String CHANNEL_ID = "com.bg";
        String CHANNEL_NAME = "TEST";
        NotificationChannel notificationChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).
                setContentTitle("相机守护正在运行").
                setContentText("别点我").
                setWhen(System.currentTimeMillis()).
                setSmallIcon(R.mipmap.ic_launcher).
                setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)).
                setContentIntent(pendingIntent).build();
        startForeground(1, notification);

    }


}
