/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yzq.zxinglibrary.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import com.yzq.zxinglibrary.R;

import java.io.Closeable;
import java.io.IOException;


/**
 * 管理声音和震动
 */
public final class BeepManager implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, Closeable {

    private static final String TAG = BeepManager.class.getSimpleName();

    private static final float BEEP_VOLUME = 0.10f;
    private static final long VIBRATE_DURATION = 200L;

    private final Activity activity;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private boolean vibrate;

    public BeepManager(Activity activity) {
        this.activity = activity;
        this.mediaPlayer = null;
        updatePrefs();
    }


    public void setPlayBeep(boolean playBeep) {
        this.playBeep = playBeep;
    }


    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public synchronized void updatePrefs() {


        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            // 设置activity音量控制键控制的音频流
            activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = buildMediaPlayer(activity);
        }
    }

    /**
     * 开启响铃和震动
     */
    @SuppressLint("MissingPermission")
    public synchronized void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) activity
                    .getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * 创建MediaPlayer
     *
     * @param activity
     * @return
     */
    private MediaPlayer buildMediaPlayer(Context activity) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // 监听是否播放完成
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        // 配置播放资源
        try {
            AssetFileDescriptor file = activity.getResources()
                    .openRawResourceFd(R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
            } finally {
                file.close();
            }
            // 设置音量
            mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mediaPlayer.release();
            return null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // When the beep has finished playing, rewind to queue up another one.
        mp.seekTo(0);
    }

    @Override
    public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // we are finished, so put up an appropriate error toast if required
            // and finish
            activity.finish();
        } else {
            // possibly media player error, so release and recreate
            mp.release();
            mediaPlayer = null;
            updatePrefs();
        }
        return true;
    }

    @Override
    public synchronized void close() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

}
