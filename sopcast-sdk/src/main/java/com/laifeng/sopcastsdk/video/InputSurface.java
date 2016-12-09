package com.laifeng.sopcastsdk.video;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

@TargetApi(18)
class InputSurface {
	//---------------------------------------------------------------------
	// CONSTANTS
	//---------------------------------------------------------------------
	private static final int EGL_RECORDABLE_ANDROID = 0x3142;

	//---------------------------------------------------------------------
	// MEMBERS
	//---------------------------------------------------------------------
	private Surface mSurface    = null;
	private EGLDisplay mEGLDisplay = null;
	private EGLContext mEGLContext = null;
	private EGLSurface mEGLSurface = null;

	//---------------------------------------------------------------------
	// PUBLIC METHODS
	//---------------------------------------------------------------------
	public InputSurface(Surface surface) {
		if (surface == null) {
			throw new NullPointerException();
		}
		mSurface = surface;
		eglSetup();
	}

	public void release() {
		EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
		EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
		EGL14.eglReleaseThread();
		EGL14.eglTerminate(mEGLDisplay);

		mSurface.release();

		mSurface    = null;
		mEGLDisplay = null;
		mEGLContext = null;
		mEGLSurface = null;
	}

	public Surface getSurface() {
		return mSurface;
	}

	public void makeCurrent() {
		if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
			throw new RuntimeException("eglMakeCurrent failed");
		}
	}

	public boolean swapBuffers() {
		return EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
	}

	public void setPresentationTime(long nsecs) {
		EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
	}

	//---------------------------------------------------------------------
	// PRIVATE...
	//---------------------------------------------------------------------
	private void eglSetup() {
		mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
		if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
			throw new RuntimeException("unable to get EGL14 display");
		}
		int[] version = new int[2];
		if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
			mEGLDisplay = null;
			throw new RuntimeException("unable to initialize EGL14");
		}

		int[] attribList = {
				EGL14.EGL_RED_SIZE, 8,
				EGL14.EGL_GREEN_SIZE, 8,
				EGL14.EGL_BLUE_SIZE, 8,
				EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
				EGL_RECORDABLE_ANDROID, 1,
				EGL14.EGL_NONE
		};
		EGLConfig[] configs = new EGLConfig[1];
		int[] numConfigs = new int[1];
		if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
				numConfigs, 0)) {
			throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
		}

		int[] attrib_list = {
				EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
				EGL14.EGL_NONE
		};
		mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.eglGetCurrentContext(),
				attrib_list, 0);
		GlUtil.checkEglError("eglCreateContext");
		if (mEGLContext == null) {
			throw new RuntimeException("null context");
		}

		int[] surfaceAttribs = {
				EGL14.EGL_NONE
		};
		mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
				surfaceAttribs, 0);
		GlUtil.checkEglError("eglCreateWindowSurface");
		if (mEGLSurface == null) {
			throw new RuntimeException("surface was null");
		}
	}
}
