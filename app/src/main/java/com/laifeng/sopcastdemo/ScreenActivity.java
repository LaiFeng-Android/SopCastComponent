package com.laifeng.sopcastdemo;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.stream.packer.rtmp.RtmpPacker;
import com.laifeng.sopcastsdk.screen.ScreenRecordActivity;
import com.laifeng.sopcastsdk.stream.sender.rtmp.RtmpSender;
import com.laifeng.sopcastsdk.utils.SopCastLog;

import static com.laifeng.sopcastsdk.constant.SopCastConstant.TAG;

public class ScreenActivity extends ScreenRecordActivity {
    private Button mStartBtn;
    private Button mStopBtn;
    private RtmpSender mRtmpSender;
    private ProgressBar mProgressConnecting;
    private VideoConfiguration mVideoConfiguration;
    private int mCurrentBps;
    private Dialog mUploadDialog;
    private EditText mAddressET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        initViews();
        initSender();
        initRtmpAddressDialog();
    }

    private void initViews() {
        mStartBtn = (Button) findViewById(R.id.startRecord);
        mStopBtn = (Button) findViewById(R.id.stopRecord);
        mProgressConnecting = (ProgressBar) findViewById(R.id.progressConnecting);

        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUploadDialog.show();
            }
        });
        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressConnecting.setVisibility(View.GONE);
                Toast.makeText(ScreenActivity.this, "stop living", Toast.LENGTH_SHORT).show();
                stopRecording();
            }
        });
    }

    private void initRtmpAddressDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View playView = inflater.inflate(R.layout.address_dialog,(ViewGroup) findViewById(R.id.dialog));
        mAddressET = (EditText) playView.findViewById(R.id.address);
        Button okBtn = (Button) playView.findViewById(R.id.ok);
        Button cancelBtn = (Button) playView.findViewById(R.id.cancel);
        AlertDialog.Builder uploadBuilder = new AlertDialog.Builder(this);
        uploadBuilder.setTitle("Upload Address");
        uploadBuilder.setView(playView);
        mUploadDialog = uploadBuilder.create();
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uploadUrl = mAddressET.getText().toString();
                if(TextUtils.isEmpty(uploadUrl)) {
                    Toast.makeText(ScreenActivity.this, "Upload address is empty!", Toast.LENGTH_SHORT).show();
                } else {
                    mRtmpSender.setAddress(uploadUrl);
                    requestRecording();
                }
                mUploadDialog.dismiss();
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUploadDialog.dismiss();
            }
        });
    }

    private void initSender() {
        mRtmpSender = new RtmpSender();
        mRtmpSender.setVideoParams(640, 360);
        mRtmpSender.setAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mRtmpSender.setSenderListener(mSenderListener);
    }

    @Override
    protected void requestRecordSuccess() {
        super.requestRecordSuccess();
        initRecorder();
        connectServer();
    }

    private void initRecorder() {
        RtmpPacker packer = new RtmpPacker();
        packer.initAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mVideoConfiguration = new VideoConfiguration.Builder().setSize(640, 360).build();
        setVideoConfiguration(mVideoConfiguration);
        setRecordPacker(packer);
        setRecordSender(mRtmpSender);
    }

    private void connectServer() {
        mProgressConnecting.setVisibility(View.VISIBLE);
        Toast.makeText(ScreenActivity.this, "start to connect server", Toast.LENGTH_SHORT).show();
        mRtmpSender.connect();
    }

    private RtmpSender.OnSenderListener mSenderListener = new RtmpSender.OnSenderListener() {
        @Override
        public void onConnecting() {

        }

        @Override
        public void onConnected() {
            mProgressConnecting.setVisibility(View.GONE);
            Toast.makeText(ScreenActivity.this, "start to upload data", Toast.LENGTH_SHORT).show();
            startRecording();
            mCurrentBps = mVideoConfiguration.maxBps;
        }

        @Override
        public void onDisConnected() {
            mProgressConnecting.setVisibility(View.GONE);
            Toast.makeText(ScreenActivity.this, "Disconnect", Toast.LENGTH_SHORT).show();
            stopRecording();
        }

        @Override
        public void onPublishFail() {
            mProgressConnecting.setVisibility(View.GONE);
            Toast.makeText(ScreenActivity.this, "Fail to publish the stream", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNetGood() {
            if (mCurrentBps + 50 <= mVideoConfiguration.maxBps){
                SopCastLog.d(TAG, "BPS_CHANGE good up 50");
                int bps = mCurrentBps + 50;
                boolean result = setRecordBps(bps);
                if(result) {
                    mCurrentBps = bps;
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
                boolean result = setRecordBps(bps);
                if(result) {
                    mCurrentBps = bps;
                }
            } else {
                SopCastLog.d(TAG, "BPS_CHANGE bad down 100");
            }
            SopCastLog.d(TAG, "Current Bps: " + mCurrentBps);
        }
    };
}
