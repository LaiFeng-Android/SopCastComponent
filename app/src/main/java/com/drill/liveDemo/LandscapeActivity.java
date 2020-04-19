package com.drill.liveDemo;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;
import android.view.WindowManager;
import android.util.Log;
import android.os.Build;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.Poi;
import com.baidu.location.PoiRegion;
import com.drill.liveDemo.ui.MultiToggleImageButton;
import com.drill.liveDemo.ui.GPSService;
import com.laifeng.sopcastsdk.camera.CameraListener;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.configuration.CameraConfiguration;
import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.entity.Watermark;
import com.laifeng.sopcastsdk.entity.WatermarkPosition;
import com.laifeng.sopcastsdk.stream.packer.rtmp.RtmpPacker;
import com.laifeng.sopcastsdk.stream.sender.rtmp.RtmpSender;
import com.laifeng.sopcastsdk.ui.CameraLivingView;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.effect.GrayEffect;
import com.laifeng.sopcastsdk.video.effect.NullEffect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.apache.http.impl.client.*;

import android.content.pm.PackageManager;

import com.drill.liveDemo.baiduGps.LocationService;

import android.app.Application;
import android.app.Service;
import android.os.Vibrator;

import static android.net.NetworkInfo.State.CONNECTED;
import static com.laifeng.sopcastsdk.constant.SopCastConstant.TAG;

public class LandscapeActivity extends Activity {
    private CameraLivingView mLFLiveView;
    private MultiToggleImageButton mFlashBtn;
    private MultiToggleImageButton mFaceBtn;
    private MultiToggleImageButton midBtn;
    private MultiToggleImageButton mgpsBtn;
    private ImageButton mbackBtn;
    private Switch mOrientationSwitch;
    private GestureDetector mGestureDetector;
    private GrayEffect mGrayEffect;
    private NullEffect mNullEffect;
    private ImageButton mRecordBtn;
    private boolean isGray;
    private boolean isRecording;
    private ProgressBar mProgressConnecting;
    private RtmpSender mRtmpSender;
    private VideoConfiguration mVideoConfiguration;
    private int mCurrentBps;
    private Dialog mUploadDialog;
    private EditText mAddressET;
    private EditText msolution;
    private String mid;
    private String mresolution;
    private boolean mProtait;
    private String mPublishUrl;

    private EditText mipEditText;
    private String mip;
    //final String defaultIP = "123.124.164.142";
    final String defaultIP = "nsw.urthe1.xyz";

    private String mdeviceID;
    private String mStatus;
    private String mNetWorkInfo;
    private int mbattery;
    private double mlongitude;//经度
    private double mlatitude;//纬度
    private String mdeviceTime;//gps时间
    private String mlocationType;//定位类型
    private float mDirection;//方向

    //动态配置信息
    private int mInterval;//上报间隔时间

    private LocationService mlocationService;
    private boolean mGpsStarted;

    private ScheduledExecutorService scheduleExecutor;
    private ScheduledFuture<?> scheduleManager;
    private Runnable timeTask;

    private Handler cameraHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0://切换摄像头
                    mLFLiveView.switchCamera();
                    break;
                case 1://切换推拉流状态
                    if (isRecording) {
                        stopLive();
                    } else {
                        startLive();
                    }
                    break;
                case 2://间隔时间
                    changeInterval(msg.arg1);
                    break;
                case 3://车牌号
                    changeCarId((String)msg.obj);
                    break;
                case 4://清晰度
                    changeResolution((String)msg.obj);
                    loadLiveViewConfig();
                    break;
                case 5:
                    changeIp((String)msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mGpsStarted = false;
        mStatus = "未推流";//当前状态
        mNetWorkInfo = "无网络";//当前网络状态
        mbattery = -1;//电池信息
        mlongitude =0;//经度
        mlatitude = 0;//纬度
        mdeviceTime ="";//gps时间
        mlocationType ="";//定位类型
        mDirection = 0;//方向
        mInterval = 10;//上报时间

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //获取预设信息
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        boolean PORTRAIT = pref.getBoolean("portrait", false);
        if (!PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_landscape);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_portrait);
        }

        init();

        Intent intent = getIntent();
        mdeviceID = intent.getStringExtra("deviceID");
        /***
         * 初始化定位sdk，建议在Application中创建
         */
        mlocationService = new LocationService(getApplicationContext());
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        mlocationService.registerListener(mListener);
        mlocationService.setLocationOption(mlocationService.getDefaultLocationClientOption());

    }

    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {

            /**
             * 定位请求回调函数
             * @param location 定位结果
             */
            @Override
                public void onReceiveLocation(BDLocation location) {

            // TODO Auto-generated method stub
            if (null != location && location.getLocType() != BDLocation.TypeServerError)
            {
                    mlongitude=location.getLongitude();
                    mlatitude=location.getLatitude();
                    mdeviceTime =location.getTime();
                    if (location.getLocType() == BDLocation.TypeGpsLocation){
                        mlocationType="GPS定位";
                    }else if(location.getLocType() == BDLocation.TypeNetWorkLocation){
                        mlocationType="网络定位";
                    }else if (location.getLocType() == BDLocation.TypeOffLineLocation){
                        mlocationType="离线定位";
                    }
                    mDirection=location.getDirection();
                }
//            {
//                int tag = 1;
//                StringBuffer sb = new StringBuffer(256);
//                sb.append("time : ");
//                /**
//                 * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
//                 * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
//                 */
//                sb.append(c);
//                sb.append("\nlocType : ");// 定位类型
//                sb.append(location.getLocType());
//                sb.append("\nlocType description : ");// *****对应的定位类型说明*****
//                sb.append(location.getLocTypeDescription());
//                sb.append("\nlatitude : ");// 纬度
//                sb.append(location.getLatitude());
//                sb.append("\nlongtitude : ");// 经度
//                sb.append(location.getLongitude());
//                sb.append("\nradius : ");// 半径
//                sb.append(location.getRadius());
//                sb.append("\nCountryCode : ");// 国家码
//                sb.append(location.getCountryCode());
//                sb.append("\nProvince : ");// 获取省份
//                sb.append(location.getProvince());
//                sb.append("\nCountry : ");// 国家名称
//                sb.append(location.getCountry());
//                sb.append("\ncitycode : ");// 城市编码
//                sb.append(location.getCityCode());
//                sb.append("\ncity : ");// 城市
//                sb.append(location.getCity());
//                sb.append("\nDistrict : ");// 区
//                sb.append(location.getDistrict());
//                sb.append("\nTown : ");// 获取镇信息
//                sb.append(location.getTown());
//                sb.append("\nStreet : ");// 街道
//                sb.append(location.getStreet());
//                sb.append("\naddr : ");// 地址信息
//                sb.append(location.getAddrStr());
//                sb.append("\nStreetNumber : ");// 获取街道号码
//                sb.append(location.getStreetNumber());
//                sb.append("\nUserIndoorState: ");// *****返回用户室内外判断结果*****
//                sb.append(location.getUserIndoorState());
//                sb.append("\nDirection(not all devices have value): ");
//                sb.append(location.getDirection());// 方向
//                sb.append("\nlocationdescribe: ");
//                sb.append(location.getLocationDescribe());// 位置语义化信息
//                sb.append("\nPoi: ");// POI信息
//                if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
//                    for (int i = 0; i < location.getPoiList().size(); i++) {
//                        Poi poi = (Poi) location.getPoiList().get(i);
//                        sb.append("poiName:");
//                        sb.append(poi.getName() + ", ");
//                        sb.append("poiTag:");
//                        sb.append(poi.getTags() + "\n");
//                    }
//                }
//                if (location.getPoiRegion() != null) {
//                    sb.append("PoiRegion: ");// 返回定位位置相对poi的位置关系，仅在开发者设置需要POI信息时才会返回，在网络不通或无法获取时有可能返回null
//                    PoiRegion poiRegion = location.getPoiRegion();
//                    sb.append("DerectionDesc:"); // 获取POIREGION的位置关系，ex:"内"
//                    sb.append(poiRegion.getDerectionDesc() + "; ");
//                    sb.append("Name:"); // 获取POIREGION的名字字符串
//                    sb.append(poiRegion.getName() + "; ");
//                    sb.append("Tags:"); // 获取POIREGION的类型
//                    sb.append(poiRegion.getTags() + "; ");
//                    sb.append("\nSDK版本: ");
//                }
//                sb.append(mlocationService.getSDKVersion()); // 获取SDK版本
//                if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
//                    sb.append("\nspeed : ");
//                    sb.append(location.getSpeed());// 速度 单位：km/h
//                    sb.append("\nsatellite : ");
//                    sb.append(location.getSatelliteNumber());// 卫星数目
//                    sb.append("\nheight : ");
//                    sb.append(location.getAltitude());// 海拔高度 单位：米
//                    sb.append("\ngps status : ");
//                    sb.append(location.getGpsAccuracyStatus());// *****gps质量判断*****
//                    sb.append("\ndescribe : ");
//                    sb.append("gps定位成功");
//                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
//                    // 运营商信息
//                    if (location.hasAltitude()) {// *****如果有海拔高度*****
//                        sb.append("\nheight : ");
//                        sb.append(location.getAltitude());// 单位：米
//                    }
//                    sb.append("\noperationers : ");// 运营商信息
//                    sb.append(location.getOperators());
//                    sb.append("\ndescribe : ");
//                    sb.append("网络定位成功");
//                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
//                    sb.append("\ndescribe : ");
//                    sb.append("离线定位成功，离线定位结果也是有效的");
//                } else if (location.getLocType() == BDLocation.TypeServerError) {
//                    sb.append("\ndescribe : ");
//                    sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
//                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
//                    sb.append("\ndescribe : ");
//                    sb.append("网络不同导致定位失败，请检查网络是否通畅");
//                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
//                    sb.append("\ndescribe : ");
//                    sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
//                }
//                Log.d("GPS",sb.toString());
//            }
        }

            @Override
            public void onConnectHotSpotMessage(String s, int i) {
            super.onConnectHotSpotMessage(s, i);
        }

            /**
             * 回调定位诊断信息，开发者可以根据相关信息解决定位遇到的一些问题
             * @param locType 当前定位类型
             * @param diagnosticType 诊断类型（1~9）
             * @param diagnosticMessage 具体的诊断信息释义
             */
            @Override
            public void onLocDiagnosticMessage(int locType, int diagnosticType, String diagnosticMessage) {
            super.onLocDiagnosticMessage(locType, diagnosticType, diagnosticMessage);
            int tag = 2;
            StringBuffer sb = new StringBuffer(256);
            sb.append("诊断结果: ");
            if (locType == BDLocation.TypeNetWorkLocation) {
                if (diagnosticType == 1) {
                    sb.append("网络定位成功，没有开启GPS，建议打开GPS会更好");
                    sb.append("\n" + diagnosticMessage);
                } else if (diagnosticType == 2) {
                    sb.append("网络定位成功，没有开启Wi-Fi，建议打开Wi-Fi会更好");
                    sb.append("\n" + diagnosticMessage);
                }
            } else if (locType == BDLocation.TypeOffLineLocationFail) {
                if (diagnosticType == 3) {
                    sb.append("定位失败，请您检查您的网络状态");
                    sb.append("\n" + diagnosticMessage);
                }
            } else if (locType == BDLocation.TypeCriteriaException) {
                if (diagnosticType == 4) {
                    sb.append("定位失败，无法获取任何有效定位依据");
                    sb.append("\n" + diagnosticMessage);
                } else if (diagnosticType == 5) {
                    sb.append("定位失败，无法获取有效定位依据，请检查运营商网络或者Wi-Fi网络是否正常开启，尝试重新请求定位");
                    sb.append(diagnosticMessage);
                } else if (diagnosticType == 6) {
                    sb.append("定位失败，无法获取有效定位依据，请尝试插入一张sim卡或打开Wi-Fi重试");
                    sb.append("\n" + diagnosticMessage);
                } else if (diagnosticType == 7) {
                    sb.append("定位失败，飞行模式下无法获取有效定位依据，请关闭飞行模式重试");
                    sb.append("\n" + diagnosticMessage);
                } else if (diagnosticType == 9) {
                    sb.append("定位失败，无法获取任何有效定位依据");
                    sb.append("\n" + diagnosticMessage);
                }
            } else if (locType == BDLocation.TypeServerError) {
                if (diagnosticType == 8) {
                    sb.append("定位失败，请确认您定位的开关打开状态，是否赋予APP定位权限");
                    sb.append("\n" + diagnosticMessage);
                }
            }
                Log.d("GPS",sb.toString());
            }
        };

    private void init(){
        initEffects();
        initViews();
        initListeners();
        initLiveView();
        initRtmpAddressDialog();
        loadLiveViewConfig();

        //初始化推流地址
        SharedPreferences pref = getSharedPreferences("data",MODE_PRIVATE);
        mid = pref.getString("id","");
        mPublishUrl = pref.getString("url","rtmp://"+mip+"/live_540/");
        if(TextUtils.isEmpty(mid)) {
            mUploadDialog.setCanceledOnTouchOutside(false);
            mUploadDialog.show();
        }

        //根据远端状态来判断
        createSchedulePool();

        //资源上报池
        createUploadPool();
    }

    private void initEffects() {
        mGrayEffect = new GrayEffect(this);
        mNullEffect = new NullEffect(this);
    }


    private void initViews() {
        mLFLiveView = (CameraLivingView) findViewById(R.id.liveView);
        mFlashBtn = (MultiToggleImageButton) findViewById(R.id.camera_flash_button);
        mFaceBtn = (MultiToggleImageButton) findViewById(R.id.camera_switch_button);
        midBtn = (MultiToggleImageButton) findViewById(R.id.id_button);
        mgpsBtn = (MultiToggleImageButton) findViewById(R.id.id_gps);
        mRecordBtn = (ImageButton) findViewById(R.id.btnRecord);
        mbackBtn = (ImageButton) findViewById(R.id.backBtn);
        mProgressConnecting = (ProgressBar) findViewById(R.id.progressConnecting);
    }

    private void createUploadPool(){
        scheduleExecutor = Executors.newScheduledThreadPool(5);
        timeTask = new Runnable() {
            @Override
            public void run() {
                //获得电量信息
                getBarryInfo();
                //获得网络状态信息
                getNetInfo();
                //上报
                uploadInfo();
            }
        };
        scheduleManager = scheduleExecutor.scheduleAtFixedRate(timeTask, 1, mInterval, TimeUnit.SECONDS);
    }

    private void getBarryInfo(){
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        mbattery =manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        Log.d("battery",String.format("battery info:%d",mbattery));
    }

    private void getNetInfo(){
        //获得ConnectivityManager对象
        Context context = getApplicationContext();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);        //获取所有网络连接的信息
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = connectivityManager.getAllNetworks();
            if (networks != null && networks.length > 0) {
                int size = networks.length;
                for (int i=0; i<size; i++) {
                   NetworkInfo.State state = connectivityManager.getNetworkInfo(networks[i]).getState();
                   if(state == CONNECTED) {
                       Log.d("TAG", "=====类型====" + connectivityManager.getNetworkInfo(networks[i]).getTypeName());
                       mNetWorkInfo = connectivityManager.getNetworkInfo(networks[i]).getTypeName();
                   }
                }
            }
        }
//        else {
//            NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();
//            if (networkInfos != null && networkInfos.length > 0) {
//                int size = networkInfos.length;
//                for (int i=0; i<size; i++) {
//                    Log.d("TAG", "=====状态====" + networkInfos[i].getState());
//                    Log.d("TAG", "=====类型====" + networkInfos[i].getTypeName());
//                }
//            }
//        }
    }

    private void createSchedulePool(){
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);
        scheduledThreadPool.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                String jsonStr = httpGet("http://drli.urthe1.xyz:8080/api/getClientStatus");
                //解析json文件
                Log.d("camera",jsonStr);

                try {
                        JSONArray jsonArray = new JSONArray(jsonStr);
                        for(int i=0;i<jsonArray.length();i++) {
                            JSONObject jsonObject=(JSONObject)jsonArray.get(i);
                            String id=jsonObject.getString("deviceID");
                            if(id.compareTo(mdeviceID)==0){//通过设备标识符找到
                                //摄像头控制
                                int facing=jsonObject.getInt("cameraPosition");
                                int cameraNow = mLFLiveView.getCameraData().cameraFacing;
                                Log.d("camera",String.format("cameraid:%d",cameraNow));
                                if(facing != cameraNow && facing!=0){
                                    cameraHandler.sendEmptyMessage(0);
                                }
                                //推流状态
                                boolean cRecord = jsonObject.getBoolean("pushStatus");
                                if(cRecord != isRecording)
                                    cameraHandler.sendEmptyMessage(1);
                                //间隔时间
                                if(!jsonObject.isNull("interval")) {
                                    int reportInterval = jsonObject.getInt("interval");
                                    if (reportInterval != mInterval) {
                                        Message msg = new Message();
                                        msg.what = 2;
                                        msg.arg1 = reportInterval;
                                        cameraHandler.sendMessage(msg);
                                    }
                                }
                                //车牌号
                                if(!jsonObject.isNull("id")){
                                    String carId = jsonObject.getString("id");
                                    if(carId != mid){
                                        Message msg= new Message();
                                        msg.what = 3;
                                        msg.obj = carId;
                                        cameraHandler.sendMessage(msg);
                                    }
                                }
                                //清晰度
                                if(!jsonObject.isNull("streamDefinition")){
                                    String resolution = jsonObject.getString("streamDefinition");
                                    if(resolution.compareTo("540P")==0){
                                        resolution = "540";
                                    }else if(resolution.compareTo(" 720P")==0){
                                        resolution = "720";
                                    }else if(resolution.compareTo(" 1080P")==0){
                                        resolution = "1080";
                                    }
                                    if(resolution != mresolution){
                                        Message msg= new Message();
                                        msg.what = 4;
                                        msg.obj = resolution;
                                        cameraHandler.sendMessage(msg);
                                    }
                                }
                                //推流地址
                                if(!jsonObject.isNull("ip")){
                                    String ip = jsonObject.getString("ip");
                                    if(ip != mip){
                                        Message msg= new Message();
                                        msg.what = 5;
                                        msg.obj = ip;
                                        cameraHandler.sendMessage(msg);
                                    }
                                }
                                break;
                            }
                        }
                    }catch (JSONException e) {
                    e.printStackTrace();
                }
         }
        }, 1, 3, TimeUnit.SECONDS);
    }

    private void initListeners() {
        mFlashBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mLFLiveView.switchTorch();
            }
        });
        mFaceBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mLFLiveView.switchCamera();
            }
        });
        midBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener(){
            public void stateChanged(View view, int state) {
                mUploadDialog.setCanceledOnTouchOutside(false);
                mAddressET.setText(mid);
                msolution.setText(mresolution);
                mOrientationSwitch.setChecked(mProtait);
                mipEditText.setText(mip);
                mUploadDialog.show();

            }
        });
        mgpsBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                if(!mGpsStarted) {
//                    Intent it = new Intent(LandscapeActivity.this,GPSService.class);
//                    startService(it);
                    mlocationService.start();// 定位SDK
                    mGpsStarted = true;
                    Toast.makeText(LandscapeActivity.this, "GPS 上报打开!", Toast.LENGTH_SHORT).show();

                }else{
//                    Intent it2 = new Intent(LandscapeActivity.this,GPSService.class);
//                    stopService(it2);
                    mlocationService.stop();// 定位SDK
                    mGpsStarted = false;
                    Toast.makeText(LandscapeActivity.this, "GPS 上报关闭!", Toast.LENGTH_SHORT).show();

                }
            }
        });
        mRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording) {
                    stopLive();
                } else {
                    startLive();
                }
            }
        });
        mbackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LandscapeActivity.this.finish();
            }
        });
    }

    private void initRtmpAddressDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View playView = inflater.inflate(R.layout.address_dialog,(ViewGroup) findViewById(R.id.dialog));
        mAddressET = (EditText) playView.findViewById(R.id.address);
        msolution = (EditText) playView.findViewById(R.id.resolution);
        mOrientationSwitch = (Switch) playView.findViewById(R.id.switchOrientation);
        mipEditText = (EditText) playView.findViewById(R.id.ip);
        Button okBtn = (Button) playView.findViewById(R.id.ok);
        Button cancelBtn = (Button) playView.findViewById(R.id.cancel);
        AlertDialog.Builder uploadBuilder = new AlertDialog.Builder(this);
        uploadBuilder.setTitle("请输入车号ID:");
        uploadBuilder.setView(playView);
        mUploadDialog = uploadBuilder.create();

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mid = mAddressET.getText().toString();
                if(TextUtils.isEmpty(mid)) {
                    Toast.makeText(LandscapeActivity.this, "车号ID不为空!", Toast.LENGTH_SHORT).show();
                    return;
                }

                mresolution = msolution.getText().toString();
                if(TextUtils.isEmpty(mresolution)) {
                    mresolution = "540";
                }

                mip = mipEditText.getText().toString();
                if(TextUtils.isEmpty(mip)) {
                    mip = defaultIP;
                }

                if(!mOrientationSwitch.isChecked())
                {
                    mProtait = false;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }else{
                    mProtait = true;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }

                //持久化
                SharedPreferences.Editor editor = getSharedPreferences("data",MODE_PRIVATE).edit();
                editor.putString("id",mid);
                editor.putString("ip",mip);
                editor.putString("resolution",mresolution);
                editor.putBoolean("portrait",mOrientationSwitch.isChecked());
                editor.apply();

                //这里需要重新导入数据
                loadLiveViewConfig();

                editor = getSharedPreferences("data",MODE_PRIVATE).edit();
                editor.putString("url",mPublishUrl);
                editor.apply();

                LandscapeActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Log.i("Dialog","dialog dismiss");
                        mUploadDialog.dismiss();
                    }
                });
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LandscapeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("Dialog","dialog dismiss");
                        mUploadDialog.dismiss();
                    }
                });                //开启状态查询
            }
        });
    }

    private void changeInterval(int newValue){
        mInterval = newValue;
        if (scheduleManager!= null)
        {
            scheduleManager.cancel(true);
        }
        scheduleManager = scheduleExecutor.scheduleAtFixedRate(timeTask, 1, mInterval, TimeUnit.SECONDS);
    }

    private void changeCarId(String carID){
        mid = carID;
        SharedPreferences.Editor editor = getSharedPreferences("data",MODE_PRIVATE).edit();
        editor.putString("id",mid);
        editor.apply();
    }
    private void changeResolution(String resolution){
        mresolution = resolution;
        SharedPreferences.Editor editor = getSharedPreferences("data",MODE_PRIVATE).edit();
        editor.putString("resolution",mresolution);
        editor.apply();
    }
    private void changeIp(String ip){
        mip = ip;
        SharedPreferences.Editor editor = getSharedPreferences("data",MODE_PRIVATE).edit();
        editor.putString("ip",mresolution);
        editor.apply();
    }

    private void stopLive(){
        mProgressConnecting.setVisibility(View.GONE);
        Toast.makeText(LandscapeActivity.this, "停止直播！", Toast.LENGTH_SHORT).show();
        mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
        mLFLiveView.stop();
        isRecording = false;
    }
    private void startLive(){
        if(TextUtils.isEmpty(mid)) {
            Toast.makeText(LandscapeActivity.this, "mid未赋值，无法推流", Toast.LENGTH_SHORT).show();
            return;
        }
        String uploadUrl = mPublishUrl+mid;
        Log.i("mid","url:"+uploadUrl);
        Toast.makeText(LandscapeActivity.this,uploadUrl, Toast.LENGTH_SHORT).show();
        mRtmpSender.setAddress(uploadUrl);
        mProgressConnecting.setVisibility(View.VISIBLE);
        Toast.makeText(LandscapeActivity.this, "start connecting", Toast.LENGTH_SHORT).show();
        mRecordBtn.setBackgroundResource(R.mipmap.ic_record_stop);
        mRtmpSender.connect();
        isRecording = true;
        mStatus = "正常";
    }

    private void initLiveView() {
        SopCastLog.isOpen(true);
        mLFLiveView.init();
        CameraConfiguration.Builder cameraBuilder = new CameraConfiguration.Builder();
        if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        {
            cameraBuilder.setOrientation(CameraConfiguration.Orientation.LANDSCAPE);
        }
        else if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        {
            cameraBuilder.setOrientation(CameraConfiguration.Orientation.PORTRAIT);
        }
        cameraBuilder.setFacing(CameraConfiguration.Facing.BACK);
        CameraConfiguration cameraConfiguration = cameraBuilder.build();
        mLFLiveView.setCameraConfiguration(cameraConfiguration);

//        //设置水印
//        Bitmap watermarkImg = BitmapFactory.decodeResource(getResources(), R.mipmap.watermark);
//        Watermark watermark = new Watermark(watermarkImg, 50, 25, WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT, 8, 8);
//        mLFLiveView.setWatermark(watermark);

        //设置预览监听
        mLFLiveView.setCameraOpenListener(new CameraListener() {
            @Override
            public void onOpenSuccess() {
                Toast.makeText(LandscapeActivity.this, "camera open success", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOpenFail(int error) {
                Toast.makeText(LandscapeActivity.this, "camera open fail", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCameraChange() {
                Toast.makeText(LandscapeActivity.this, "camera switch", Toast.LENGTH_LONG).show();
            }
        });

        //设置手势识别
        mGestureDetector = new GestureDetector(this, new GestureListener());
        mLFLiveView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return false;
            }
        });

        //初始化flv打包器
        RtmpPacker packer = new RtmpPacker();
        packer.initAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mLFLiveView.setPacker(packer);
        //设置发送器
        mRtmpSender = new RtmpSender();
        mRtmpSender.setAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mRtmpSender.setSenderListener(mSenderListener);
        mLFLiveView.setSender(mRtmpSender);
        mLFLiveView.setLivingStartListener(new CameraLivingView.LivingStartListener() {
            @Override
            public void startError(int error) {
                //直播失败
                Toast.makeText(LandscapeActivity.this, "start living fail", Toast.LENGTH_SHORT).show();
                mLFLiveView.stop();
            }

            @Override
            public void startSuccess() {
                //直播成功
                Toast.makeText(LandscapeActivity.this, "开始直播,id号:"+mid+",地址:"+mPublishUrl, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLiveViewConfig(){
        SharedPreferences pref = getSharedPreferences("data",MODE_PRIVATE);
        mProtait = pref.getBoolean("portrait",false);
        mip = pref.getString("ip",defaultIP);
        mresolution  = pref.getString("resolution","540");

        if(!mProtait)
        {
            if(mresolution.compareTo("1080")==0){
                VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
                videoBuilder.setSize(1920, 1080).setBps(900,1800);
                mVideoConfiguration = videoBuilder.build();
                mLFLiveView.setVideoConfiguration(mVideoConfiguration);
                mRtmpSender.setVideoParams(1920, 1080);

                mPublishUrl = "rtmp://"+mip+"/live_landscape_1080p/";
            }else if (mresolution.compareTo("720")==0){
                VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
                videoBuilder.setSize(1280, 720).setBps(600,1600);
                mVideoConfiguration = videoBuilder.build();
                mLFLiveView.setVideoConfiguration(mVideoConfiguration);

                mRtmpSender.setVideoParams(1280, 720);

                mPublishUrl = "rtmp://"+mip+"/live_720_convert/";
            }else{
                Toast.makeText(LandscapeActivity.this, "默认用540", Toast.LENGTH_SHORT).show();
                VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
                videoBuilder.setSize(960, 540).setBps(450,1200);
                mVideoConfiguration = videoBuilder.build();
                mLFLiveView.setVideoConfiguration(mVideoConfiguration);

                mRtmpSender.setVideoParams(960, 540);

                mPublishUrl = "rtmp://"+mip+"/live_540/";
            }
        }else{
            if(mresolution.compareTo("1080")==0){
                VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
                videoBuilder.setSize(1080, 1920).setBps(900,1800);
                mVideoConfiguration = videoBuilder.build();
                mLFLiveView.setVideoConfiguration(mVideoConfiguration);

                mRtmpSender.setVideoParams(1080, 1920);

                mPublishUrl = "rtmp://"+mip+"/live_portrait_1080p/";
            }else if (mresolution.compareTo("720")==0){
                VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
                videoBuilder.setSize(720, 1280).setBps(600,1600);
                mVideoConfiguration = videoBuilder.build();
                mLFLiveView.setVideoConfiguration(mVideoConfiguration);

                mRtmpSender.setVideoParams(720, 1280);

                mPublishUrl = "rtmp://"+mip+"/live_portrait_720p/";
            }else{
                Toast.makeText(LandscapeActivity.this, "默认用540", Toast.LENGTH_SHORT).show();
                VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
                videoBuilder.setSize(540, 960).setBps(450,1200);
                mVideoConfiguration = videoBuilder.build();
                mLFLiveView.setVideoConfiguration(mVideoConfiguration);

                mRtmpSender.setVideoParams(540, 960);

                mPublishUrl = "rtmp://"+mip+"/live_540/";
            }
        }
    }

    private RtmpSender.OnSenderListener mSenderListener = new RtmpSender.OnSenderListener() {
        @Override
        public void onConnecting() {

        }

        @Override
        public void onConnected() {
            mProgressConnecting.setVisibility(View.GONE);
            mLFLiveView.start();
            mCurrentBps = mVideoConfiguration.maxBps;
        }

        @Override
        public void onDisConnected() {
            mProgressConnecting.setVisibility(View.GONE);
            Toast.makeText(LandscapeActivity.this, "fail to live", Toast.LENGTH_SHORT).show();
            mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
            mLFLiveView.stop();
            isRecording = false;
        }

        @Override
        public void onPublishFail() {
            mProgressConnecting.setVisibility(View.GONE);
            Toast.makeText(LandscapeActivity.this, "fail to publish stream", Toast.LENGTH_SHORT).show();
            mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
            isRecording = false;
        }

        @Override
        public void onNetGood() {
            if (mCurrentBps + 50 <= mVideoConfiguration.maxBps){
                SopCastLog.d(TAG, "BPS_CHANGE good up 50");
                int bps = mCurrentBps + 50;
                if(mLFLiveView != null) {
                    boolean result = mLFLiveView.setVideoBps(bps);
                    if(result) {
                        mCurrentBps = bps;
                    }
                }
            } else {
                SopCastLog.d(TAG, "BPS_CHANGE good good good");
            }
            SopCastLog.d(TAG, "Current Bps: " + mCurrentBps);
        }

        @Override
        public void onNetBad() {
            if (mCurrentBps - 100 >= mVideoConfiguration.minBps){
                SopCastLog.d(TAG, "BPS_CHANGE bad down 100");
                int bps = mCurrentBps - 100;
                if(mLFLiveView != null) {
                    boolean result = mLFLiveView.setVideoBps(bps);
                    if(result) {
                        mCurrentBps = bps;
                    }
                }
            } else {
                SopCastLog.d(TAG, "BPS_CHANGE bad down 100");
            }
            SopCastLog.d(TAG, "Current Bps: " + mCurrentBps);
        }
    };

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > 100
                    && Math.abs(velocityX) > 200) {
                // Fling left
                Toast.makeText(LandscapeActivity.this, "Fling Left", Toast.LENGTH_SHORT).show();
            } else if (e2.getX() - e1.getX() > 100
                    && Math.abs(velocityX) > 200) {
                // Fling right
                Toast.makeText(LandscapeActivity.this, "Fling Right", Toast.LENGTH_SHORT).show();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mLFLiveView!=null)
            mLFLiveView.pause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mLFLiveView!=null){
            mLFLiveView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLFLiveView.stop();
        mLFLiveView.release();
    }

    public String httpGet( String httpUrl ){
        String result = "" ;
        try {
            BufferedReader reader = null;
            StringBuffer sbf = new StringBuffer() ;

            URL url  = new URL( httpUrl ) ;
            HttpURLConnection connection = (HttpURLConnection) url.openConnection() ;
            //设置超时时间 10s
            connection.setConnectTimeout(10000);
            //设置请求方式
            connection.setRequestMethod( "GET" ) ;
            connection.connect();
            InputStream is = connection.getInputStream() ;
            reader = new BufferedReader(new InputStreamReader( is , "UTF-8" )) ;
            String strRead = null ;
            while ((strRead = reader.readLine()) != null) {
                sbf.append(strRead);
                sbf.append("\r\n");
            }
            reader.close();
            result = sbf.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    private void uploadInfo()
    {
        new Thread(new Runnable() {
            public void run() {
                String uriAPI = "http://drli.urthe1.xyz:8080/api/updateDevicesStatus?deviceID=" + mdeviceID+"&streamID="+mid;
                if(!TextUtils.isEmpty(mStatus)){
                    uriAPI += String.format("&appStatus=%s",mStatus);
                }
                if(!TextUtils.isEmpty(mNetWorkInfo)){
                    uriAPI += String.format("&networkType=%s",mNetWorkInfo);
                }
                if(mbattery > 0){
                    uriAPI += String.format("&battery=%d",mbattery);
                }
                if(mlongitude >0){
                    uriAPI += String.format("&longitude=%f",mlongitude);
                }
                if(mlatitude >0){
                    uriAPI += String.format("&latitude=%f",mlatitude);
                }
                if(!TextUtils.isEmpty(mlocationType)){
                    uriAPI += String.format("&locationType=%s",mlocationType);
                }
                if(mDirection > 0){
                    uriAPI += String.format("&direction=%f",mDirection);
                }

                HttpClient postClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(uriAPI);
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                if(!TextUtils.isEmpty(mdeviceTime)){
                    params.add(new BasicNameValuePair("deviceTime", mdeviceTime));
                }

                UrlEncodedFormEntity entity;
                HttpResponse response;
                try {
                    entity = new UrlEncodedFormEntity(params, "utf-8");
                    httpPost.setEntity(entity);
                    response = postClient.execute(httpPost);

                    if (response.getStatusLine().getStatusCode() == 200) {

                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ;
            }
        }).start();

    }

    protected void onPause(){
        super.onPause();
        Log.e("active","active:pause");
        mStatus = "注意，设备已经切后台！！！";
    }

    protected void onResume(){
        super.onResume();
        Log.e("active","active:resume");
    }
}
