package com.laifeng.sopcastsdk.camera.focus;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import com.laifeng.sopcastsdk.R;
import com.laifeng.sopcastsdk.camera.CameraData;
import com.laifeng.sopcastsdk.camera.CameraHolder;

/**
 * Focus ring HUD that lets user select focus point (tap to focus)
 */
@TargetApi(18)
public class FocusPieView extends ImageView {

    public FocusPieView(Context context) {
        super(context);
    }

    public FocusPieView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusPieView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setFocusImage(true);
    }

    public void setFocusImage(boolean success) {
        if (success) {
            setImageResource(R.drawable.camera_focus_ring_success);
        } else {
            setImageResource(R.drawable.camera_focus_ring_fail);
        }
    }

    /**
     * Centers the focus ring on the x,y coordinates provided
     * and sets the focus to this position
     *
     * @param x
     * @param y
     */
    public void setPosition(float x, float y) {
        setX(x - getWidth() / 2.0f);
        setY(y - getHeight() / 2.0f);
        applyFocusPoint();
    }

    public void resetPosition() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return;
        setPosition(parent.getWidth()/2, parent.getHeight()/2);
    }

    private void applyFocusPoint() {
        ViewGroup parent = (ViewGroup) getParent();

        if (parent == null) return;
        CameraData cameraData = CameraHolder.instance().getCameraData();
        if(cameraData == null) return;

        int cameraWidth, cameraHeight;
        if(CameraHolder.instance().isLandscape()) {
            cameraWidth  = cameraData.cameraWidth;
            cameraHeight  = cameraData.cameraHeight;
        } else {
            cameraWidth  = cameraData.cameraHeight;
            cameraHeight  = cameraData.cameraWidth;
        }

        float hRatio = parent.getWidth() / ((float)cameraWidth);
        float vRatio = parent.getHeight() / ((float)cameraHeight);

        float centerPointX;
        float centerPointY;

        if(hRatio > vRatio) {
            cameraWidth = parent.getWidth();
            cameraHeight = (int)(cameraHeight * hRatio);
            int margin = (cameraHeight - parent.getHeight())/2;

            if(CameraHolder.instance().isLandscape()) {

                centerPointX = getX() + getWidth() / 2.0f;
                centerPointY = getY() + getHeight() / 2.0f + margin;

                centerPointX *= 1000.0f / cameraWidth;
                centerPointY *= 1000.0f / cameraHeight;
            } else {
                // We swap X/Y as we have a landscape preview in portrait mode
                centerPointX = getY() + getHeight() / 2.0f + margin;
                centerPointY = parent.getWidth() - (getX() + getWidth() / 2.0f);

                centerPointX *= 1000.0f / cameraHeight;
                centerPointY *= 1000.0f / cameraWidth;
            }
        } else {
            cameraWidth = (int)(cameraWidth * vRatio);
            cameraHeight = parent.getHeight();
            int margin = (cameraWidth - parent.getWidth())/2;

            if(CameraHolder.instance().isLandscape()) {

                centerPointX = getX() + getWidth() / 2.0f + margin;
                centerPointY = getY() + getHeight() / 2.0f;

                centerPointX *= 1000.0f / cameraWidth;
                centerPointY *= 1000.0f / cameraHeight;
            } else {
                // We swap X/Y as we have a landscape preview in portrait mode
                centerPointX = getY() + getHeight() / 2.0f;
                centerPointY = parent.getWidth() - (getX() + getWidth() / 2.0f) + margin;

                centerPointX *= 1000.0f / cameraHeight;
                centerPointY *= 1000.0f / cameraWidth;
            }
        }

        centerPointX = (centerPointX - 500.0f) * 2.0f;
        centerPointY = (centerPointY - 500.0f) * 2.0f;

        // The CamManager might be null if users try to tap the preview area, when the
        // camera is actually not yet ready
        CameraHolder.instance().setFocusPoint((int) centerPointX, (int) centerPointY);
    }

    public void animateWorking(long duration) {
        animate().rotationBy(45.0f).setDuration(duration).setInterpolator(
                new DecelerateInterpolator()).start();
    }
}
