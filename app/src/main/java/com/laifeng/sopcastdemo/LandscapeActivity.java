package com.laifeng.sopcastdemo;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.WindowManager;
import android.util.Log;

import com.laifeng.sopcastdemo.ui.MultiToggleImageButton;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.laifeng.sopcastsdk.constant.SopCastConstant.TAG;

public class LandscapeActivity extends Activity {
    private CameraLivingView mLFLiveView;
    private MultiToggleImageButton mFlashBtn;
    private MultiToggleImageButton mFaceBtn;
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

    private Handler cameraHandler = new Handler(){
    @Override
    public void handleMessage(Message msg) {
        switch(msg.what){
        case 0://切换摄像头
            mLFLiveView.switchCamera();
            break;
        case 1://切换推拉流状态
            if(isRecording) {
                stopLive();
            } else {
                startLive();
            }
            break;
        default:
            break;
        }
    }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_landscape);
        initEffects();
        initViews();
        initListeners();
        initLiveView();
        initRtmpAddressDialog();
        mUploadDialog.show();
    }

    private void initEffects() {
        mGrayEffect = new GrayEffect(this);
        mNullEffect = new NullEffect(this);
    }


    private void initViews() {
        mLFLiveView = (CameraLivingView) findViewById(R.id.liveView);
        mFlashBtn = (MultiToggleImageButton) findViewById(R.id.camera_flash_button);
        mFaceBtn = (MultiToggleImageButton) findViewById(R.id.camera_switch_button);
        mRecordBtn = (ImageButton) findViewById(R.id.btnRecord);
        mProgressConnecting = (ProgressBar) findViewById(R.id.progressConnecting);
    }

    private void createSchedulePool(){
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);
        scheduledThreadPool.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                String jsonStr = httpGet("http://www.mockhttp.cn/mock/camera");
                //解析json文件
                Log.d("camera",jsonStr);

                try {
                    //摄像头控制
                    JSONArray jsonArray = new JSONObject(jsonStr).getJSONArray("cameraInfo");
                    for(int i=0;i<jsonArray.length();i++) {
                        JSONObject jsonObject=(JSONObject)jsonArray.get(i);
                        String id=jsonObject.getString("id");
                        if(id.compareTo(mid)==0){//找到车辆
                            int facing=jsonObject.getInt("camera");
                            int cameraNow = mLFLiveView.getCameraData().cameraFacing;
                            Log.d("camera",String.format("cameraid:%d",cameraNow));
                            if(facing != cameraNow){
                                cameraHandler.sendEmptyMessage(0);//0切换摄像头
                            }
                            break;
                        }
                    }
                    //推拉流控制
                    JSONArray jsonArray2 = new JSONObject(jsonStr).getJSONArray("streamInfo");
                    for(int i=0;i<jsonArray2.length();i++) {
                        JSONObject jsonObject=(JSONObject)jsonArray2.get(i);
                        String id=jsonObject.getString("id");
                        if(id.compareTo(mid)==0) {//找到车辆
                            boolean cRecord = jsonObject.getBoolean("push");
                            if(cRecord != isRecording)
                                cameraHandler.sendEmptyMessage(1);//1切换推流状态
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
    }

    private void initRtmpAddressDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View playView = inflater.inflate(R.layout.address_dialog,(ViewGroup) findViewById(R.id.dialog));
        mAddressET = (EditText) playView.findViewById(R.id.address);
        msolution = (EditText) playView.findViewById(R.id.resolution);
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
                //初始化参数
                mresolution = msolution.getText().toString();
                if(TextUtils.isEmpty(mresolution)){
                    Toast.makeText(LandscapeActivity.this, "分辨率为空，默认用1080!", Toast.LENGTH_SHORT).show();
                }else{
                    if(mresolution.compareTo("540")==0){
                        VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
                        videoBuilder.setSize(960, 540).setBps(450,1200);
                        mVideoConfiguration = videoBuilder.build();
                        mLFLiveView.setVideoConfiguration(mVideoConfiguration);

                        mRtmpSender.setVideoParams(960, 540);

                    }else if (mresolution.compareTo("720")==0){
                        VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
                        videoBuilder.setSize(1280, 720).setBps(600,1600);
                        mVideoConfiguration = videoBuilder.build();
                        mLFLiveView.setVideoConfiguration(mVideoConfiguration);

                        mRtmpSender.setVideoParams(1280, 720);

                    }
                }
                //开始推流
                startLive();
                mUploadDialog.dismiss();
                //创建轮询池
                //createSchedulePool();
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUploadDialog.dismiss();
            }
        });
    }

    private void stopLive(){
        mProgressConnecting.setVisibility(View.GONE);
        Toast.makeText(LandscapeActivity.this, "stop living", Toast.LENGTH_SHORT).show();
        mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
        mLFLiveView.stop();
        isRecording = false;
    }
    private void startLive(){
        String uploadUrl;
        String zero = "0";
        if(mid.compareTo(zero) == 0){
            uploadUrl = "rtmp://39.106.49.206:1935/live/";
        }else{
            uploadUrl = "rtmp://39.106.49.206:1935/live"+mid+"/";
        }
        Toast.makeText(LandscapeActivity.this,uploadUrl, Toast.LENGTH_SHORT).show();
        mRtmpSender.setAddress(uploadUrl);
        mProgressConnecting.setVisibility(View.VISIBLE);
        Toast.makeText(LandscapeActivity.this, "start connecting", Toast.LENGTH_SHORT).show();
        mRecordBtn.setBackgroundResource(R.mipmap.ic_record_stop);
        mRtmpSender.connect();
        isRecording = true;
    }

    private void initLiveView() {
        SopCastLog.isOpen(true);
        mLFLiveView.init();
        CameraConfiguration.Builder cameraBuilder = new CameraConfiguration.Builder();
        cameraBuilder.setOrientation(CameraConfiguration.Orientation.LANDSCAPE)
                .setFacing(CameraConfiguration.Facing.BACK);
        CameraConfiguration cameraConfiguration = cameraBuilder.build();
        mLFLiveView.setCameraConfiguration(cameraConfiguration);

        VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
        videoBuilder.setSize(1920, 1080).setBps(900,2400);
        mVideoConfiguration = videoBuilder.build();
        mLFLiveView.setVideoConfiguration(mVideoConfiguration);

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
        mRtmpSender.setVideoParams(1920, 1080);
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
                Toast.makeText(LandscapeActivity.this, "start living", Toast.LENGTH_SHORT).show();
            }
        });
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
        mLFLiveView.pause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLFLiveView.resume();
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

    protected void onPause(){
        super.onPause();
        Log.e("active","active:pause");
    }

    protected void onResume(){
        super.onResume();
        Log.e("active","active:resume");
    }
}
