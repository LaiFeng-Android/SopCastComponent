package com.laifeng.sopcastsdk.stream.sender.local;

import android.os.Environment;

import com.laifeng.sopcastsdk.stream.sender.Sender;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Title: LocalSender
 * @Package com.laifeng.sopcastsdk.stream.sender.local
 * @Description:
 * @Author Jim
 * @Date 16/9/18
 * @Time 下午5:10
 * @Version
 */
public class LocalSender implements Sender{
    private File mTestFile;
    private FileOutputStream mOutStream;
    private static BufferedOutputStream mBuffer;

    @Override
    public void start() {
        String sdcardPath = Environment.getExternalStorageDirectory().toString();
        mTestFile = new File(sdcardPath+"/SopCast.flv");

        if(mTestFile.exists()){
            mTestFile.delete();
        }

        try {
            mTestFile.createNewFile();
            mOutStream = new FileOutputStream(mTestFile);
            mBuffer = new BufferedOutputStream(mOutStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onData(byte[] data, int type) {
        if (mBuffer != null){
            try {
                mBuffer.write(data);
                mBuffer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if(mBuffer != null) {
            try {
                mBuffer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mBuffer = null;
        }
        if(mOutStream != null) {
            try {
                mOutStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mBuffer = null;
            mOutStream = null;
        }
    }
}
