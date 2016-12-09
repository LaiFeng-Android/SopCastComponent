package com.laifeng.sopcastsdk.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.laifeng.sopcastsdk.R;
import com.laifeng.sopcastsdk.camera.CameraData;
import com.laifeng.sopcastsdk.camera.CameraHolder;
import com.laifeng.sopcastsdk.camera.CameraZoomListener;
import com.laifeng.sopcastsdk.camera.focus.FocusManager;
import com.laifeng.sopcastsdk.camera.focus.FocusPieView;
import com.laifeng.sopcastsdk.utils.WeakHandler;
import com.laifeng.sopcastsdk.video.MyRenderer;

/**
 * @Title: CameraView
 * @Package com.laifeng.sopcastsdk.ui
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午5:31
 * @Version
 */
public class CameraView extends FrameLayout {
    private Context mContext;
    protected RenderSurfaceView mRenderSurfaceView;
    protected MyRenderer mRenderer;
    private FocusPieView mFocusHudRing;
    private FocusManager mFocusManager;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mZoomGestureDetector;
    private WeakHandler mHandler;
    private boolean mIsFocusing;
    private CameraZoomListener mZoomListener;
    private boolean isFocusTouchMode = false;
    private boolean isMediaOverlay;
    private boolean isRenderSurfaceViewShowing = true;
    private float mAspectRatio = 9.0f/16;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();
        initAspectRatio(attrs);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();
        initAspectRatio(attrs);
    }

    public CameraView(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    private void initView() {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.layout_camera_view, this, true);
        mHandler = new WeakHandler();
        mRenderSurfaceView = (RenderSurfaceView) findViewById(R.id.render_surface_view);
        mRenderSurfaceView.setZOrderMediaOverlay(isMediaOverlay);
        mRenderer = mRenderSurfaceView.getRenderer();
        mFocusHudRing = (FocusPieView) findViewById(R.id.focus_view);
        mFocusManager = new FocusManager();
        mFocusManager.setListener(new MainFocusListener());
        mGestureDetector = new GestureDetector(mContext, new GestureListener());
        mZoomGestureDetector = new ScaleGestureDetector(mContext, new ZoomGestureListener());
    }

    private void initAspectRatio(AttributeSet attrs) {
        TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.CameraLivingView);
        mAspectRatio = a.getFloat(R.styleable.CameraLivingView_aspect_ratio, 9.0f / 16);
    }

    public void setOnZoomProgressListener(CameraZoomListener listener) {
        mZoomListener = listener;
    }

    @Override
    public void setVisibility(int visibility) {
        int currentVisibility = getVisibility();
        if(visibility == currentVisibility) {
            return;
        }
        switch (visibility) {
            case VISIBLE:
                addRenderSurfaceView();
                break;
            case GONE:
                removeRenderSurfaceView();
                break;
            case INVISIBLE:
                removeRenderSurfaceView();
                break;
        }
        super.setVisibility(visibility);
    }

    private void addRenderSurfaceView() {
        if(!isRenderSurfaceViewShowing) {
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            addView(mRenderSurfaceView, 0, layoutParams);
            isRenderSurfaceViewShowing = true;
        }
    }

    private void removeRenderSurfaceView() {
        if(isRenderSurfaceViewShowing) {
            removeView(mRenderSurfaceView);
            isRenderSurfaceViewShowing = false;
        }
    }

    /**
     * Focus listener to animate the focus HUD ring from FocusManager events
     */
    private class MainFocusListener implements FocusManager.FocusListener {
        @Override
        public void onFocusStart() {
            mIsFocusing = true;
            mFocusHudRing.setVisibility(VISIBLE);
            mFocusHudRing.animateWorking(1500);
            requestLayout();
        }

        @Override
        public void onFocusReturns(final boolean success) {
            mIsFocusing = false;
            mFocusHudRing.setFocusImage(success);
            mFocusHudRing.setVisibility(INVISIBLE);
            requestLayout();
        }
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mFocusManager != null) {
                mFocusHudRing.setPosition(e.getX(), e.getY());
                mFocusManager.refocus();
            }
            return super.onSingleTapConfirmed(e);
        }
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay) {
        this.isMediaOverlay = isMediaOverlay;
        if(mRenderSurfaceView != null) {
            mRenderSurfaceView.setZOrderMediaOverlay(isMediaOverlay);
        }
    }

    /**
     * Handles the pinch-to-zoom gesture
     */
    private class ZoomGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!mIsFocusing) {
                float progress = 0;
                if (detector.getScaleFactor() > 1.0f) {
                    progress = CameraHolder.instance().cameraZoom(true);
                } else if (detector.getScaleFactor() < 1.0f) {
                    progress = CameraHolder.instance().cameraZoom(false);
                } else {
                    return false;
                }
                if(mZoomListener != null) {
                    mZoomListener.onZoomProgress(progress);
                }
            }
            return true;
        }
    }

    protected void changeFocusModeUI() {
        CameraData cameraData = CameraHolder.instance().getCameraData();
        if(cameraData != null && cameraData.supportTouchFocus && cameraData.touchFocusMode) {
            isFocusTouchMode = true;
            if (mFocusManager != null) {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mFocusHudRing.resetPosition();
                        mFocusManager.refocus();
                    }
                }, 1000);
            }
        } else {
            isFocusTouchMode = false;
            mFocusHudRing.setVisibility(INVISIBLE);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isFocusTouchMode) {
            return mGestureDetector.onTouchEvent(event) || mZoomGestureDetector.onTouchEvent(event);
        } else {
            return mZoomGestureDetector.onTouchEvent(event);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        if(widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.AT_MOST) {
            heightSpecSize = (int)(widthSpecSize / mAspectRatio);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSpecSize,
                    MeasureSpec.EXACTLY);
        } else if(widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.EXACTLY) {
            widthSpecSize = (int)(heightSpecSize * mAspectRatio);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSpecSize,
                    MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
