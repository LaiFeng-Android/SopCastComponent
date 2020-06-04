package com.drill.liveDemo.ui;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

class GPSServiceListener implements LocationListener {
    @Override
    public void onLocationChanged(Location location) {
        Log.i("GPSService", "时间：" + location.getTime());
        Log.i("GPSService", "经度：" + location.getLongitude());
        Log.i("GPSService", "纬度：" + location.getLatitude());
        Log.i("GPSService", "海拔：" + location.getAltitude());
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i("GPSService", "onProviderDisabled");
        //  Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i("GPSService", "onProviderEnabled");

        //  Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("GPSService", "onStatusChanged");
    }
}


public class GPSService extends Service {

    private static final long minTime = 20000;

    private static final float minDistance = 10;

    String tag = this.toString();

    private LocationManager locationManager;

    private LocationListener locationListener;

    private final IBinder mBinder = new GPSServiceBinder();

    public void startService() {
        Log.d("GPSService", "GPS services started");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new GPSServiceListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("GPSService", "GPS services permission not allowed");
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance,
                locationListener);
    }

    public void endService() {
        Log.d("GPSService","GPS services end");
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }


    @Override
    public IBinder onBind(Intent arg0) {
        //  Auto-generated method stub
        return mBinder;
    }


    @Override
    public void onCreate() {
        //
        startService();
        Log.v(tag, "GPSService Started.");
    }

    @Override
    public void onDestroy() {
        endService();
        Log.v(tag, "GPSService Ended.");
    }

    public class GPSServiceBinder extends Binder {
        GPSService getService() {
            return GPSService.this;
        }
    }
}
