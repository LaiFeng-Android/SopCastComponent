package com.laifeng.sopcastsdk.camera.focus;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;

import com.laifeng.sopcastsdk.camera.CameraHolder;

public class FocusManager implements AutoFocusCallback {
    public final static String TAG = "FocusManager";

    public interface FocusListener {
        void onFocusStart();
        void onFocusReturns(boolean success);
    }

    private FocusListener mListener;

    public void setListener(FocusListener listener) {
        mListener = listener;
    }

    public void refocus() {
        boolean focusResult = CameraHolder.instance().doAutofocus(this);
        if (focusResult) {
            if (mListener != null) {
                mListener.onFocusStart();
            }
        }
    }

    @Override
    public void onAutoFocus(boolean success, Camera cam) {
        if (mListener != null) {
            mListener.onFocusReturns(success);
        }
    }
}
