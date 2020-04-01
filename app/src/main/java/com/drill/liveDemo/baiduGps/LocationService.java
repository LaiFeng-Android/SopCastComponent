package com.drill.liveDemo.baiduGps;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

import android.content.Context;
import android.webkit.WebView;

/**
 * 定位服务LocationClient 相关
 *
 * @author baidu
 */
public class LocationService {
    private static LocationClient client = null;
    private static LocationClientOption mOption;
    private static LocationClientOption  DIYoption;
    private Object objLock;

    /***
     * 初始化 LocationClient
     *
     * @param locationContext
     */
    public LocationService(Context locationContext) {
        objLock = new Object();
        synchronized (objLock) {
            if (client == null) {
                client = new LocationClient(locationContext);
                client.setLocOption(getDefaultLocationClientOption());
            }
        }
    }

    /***
     * 注册定位监听
     *
     * @param listener
     * @return
     */

    public boolean registerListener(BDAbstractLocationListener listener) {
        boolean isSuccess = false;
        if (listener != null) {
            client.registerLocationListener(listener);
            isSuccess = true;
        }
        return isSuccess;
    }

    public void unregisterListener(BDAbstractLocationListener listener) {
        if (listener != null) {
            client.unRegisterLocationListener(listener);
        }
    }

    /**
     * @return 获取sdk版本
     */
    public String getSDKVersion() {
        if (client != null) {
            String version = client.getVersion();
            return version;
        }
        return null;
    }

    /***
     * 设置定位参数
     *
     * @param option
     * @return isSuccessSetOption
     */
    public static boolean setLocationOption(LocationClientOption option) {
        boolean isSuccess = false;
        if (option != null) {
            if (client.isStarted()) {
                client.stop();
            }
            DIYoption = option;
            client.setLocOption(option);
        }
        return isSuccess;
    }

    /**
     * 开发者应用如果有H5页面使用了百度JS接口，该接口可以辅助百度JS更好的进行定位
     *
     * @param webView 传入webView控件
     */
    public void enableAssistanLocation(WebView webView) {
        if (client != null) {
            client.enableAssistantLocation(webView);
        }
    }

    /**
     * 停止H5辅助定位
     */
    public void disableAssistantLocation() {
        if (client != null) {
            client.disableAssistantLocation();
        }
    }

    /***
     *
     * @return DefaultLocationClientOption  默认O设置
     */
    public LocationClientOption getDefaultLocationClientOption() {
        if (mOption == null) {
            mOption = new LocationClientOption();
            mOption.setLocationMode(LocationMode.Hight_Accuracy); // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            mOption.setCoorType( "bd09ll" ); // 可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
            mOption.setScanSpan(20000); // 可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
            mOption.setIsNeedAddress(true); // 可选，设置是否需要地址信息，默认不需要
            mOption.setIsNeedLocationDescribe(true); // 可选，设置是否需要地址描述
            mOption.setNeedDeviceDirect(false); // 可选，设置是否需要设备方向结果
            mOption.setLocationNotify(false); // 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            mOption.setIgnoreKillProcess(true); // 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop
            mOption.setIsNeedLocationDescribe(true); // 可选，默认false，设置是否需要位置语义化结果，可以在BDLocation
            mOption.setIsNeedLocationPoiList(true); // 可选，默认false，设置是否需要POI结果，可以在BDLocation
            mOption.SetIgnoreCacheException(false); // 可选，默认false，设置是否收集CRASH信息，默认收集
            mOption.setOpenGps(true); // 可选，默认false，设置是否开启Gps定位
            mOption.setIsNeedAltitude(false); // 可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        }
        return mOption;
    }


    /**
     * @return DIYOption 自定义Option设置
     */
    public LocationClientOption getOption() {
        if (DIYoption == null) {
            DIYoption = new LocationClientOption();
        }
        return DIYoption;
    }

    public void start() {
        synchronized (objLock) {
            if (client != null && !client.isStarted()) {
                client.start();
            }
        }
    }

    public void requestLocation() {
        if (client != null) {
            client.requestLocation();
        }
    }

    public void stop() {
        synchronized (objLock) {
            if (client != null && client.isStarted()) {
                client.stop();
            }
        }
    }

    public boolean isStart() {
        return client.isStarted();
    }

    public boolean requestHotSpotState() {
        return client.requestHotSpotState();
    }
}
