package com.laifeng.sopcastsdk.stream.packer.rtmp;

import android.media.MediaCodec;

import com.laifeng.sopcastsdk.stream.packer.AnnexbHelper;
import com.laifeng.sopcastsdk.stream.packer.Packer;
import com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper;
import java.nio.ByteBuffer;

import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.AUDIO_HEADER_SIZE;
import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.AUDIO_SPECIFIC_CONFIG_SIZE;
import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.VIDEO_HEADER_SIZE;
import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.VIDEO_SPECIFIC_CONFIG_EXTEND_SIZE;

/**
 * @Title: FlvPacker
 * @Package com.laifeng.sopcastsdk.stream.packer
 * @Description:
 * @Author Jim
 * @Date 16/9/13
 * @Time 上午11:51
 * @Version
 */
public class RtmpPacker implements Packer, AnnexbHelper.AnnexbNaluListener{

    public static final int FIRST_VIDEO = 1;
    public static final int FIRST_AUDIO = 2;
    public static final int AUDIO = 3;
    public static final int KEY_FRAME = 4;
    public static final int INTER_FRAME = 5;
    public static final int CONFIGRATION = 6;

    private OnPacketListener packetListener;
    private boolean isHeaderWrite;
    private boolean isKeyFrameWrite;

    private int mAudioSampleRate, mAudioSampleSize;
    private boolean mIsStereo;

    private AnnexbHelper mAnnexbHelper;

    public RtmpPacker() {
        mAnnexbHelper = new AnnexbHelper();
    }

    @Override
    public void setPacketListener(OnPacketListener listener) {
        packetListener = listener;
    }

    @Override
    public void start() {
        mAnnexbHelper.setAnnexbNaluListener(this);
    }

    @Override
    public void onVideoData(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        mAnnexbHelper.analyseVideoData(bb, bi);
    }

    @Override
    public void onAudioData(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        if(packetListener == null || !isHeaderWrite || !isKeyFrameWrite) {
            return;
        }
        bb.position(bi.offset);
        bb.limit(bi.offset + bi.size);

        byte[] audio = new byte[bi.size];
        bb.get(audio);
        int size = AUDIO_HEADER_SIZE + audio.length;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeAudioTag(buffer, audio, false, mAudioSampleSize);
        packetListener.onPacket(buffer.array(), AUDIO);
    }

    @Override
    public void stop() {
        isHeaderWrite = false;
        isKeyFrameWrite = false;
        mAnnexbHelper.stop();
    }

    @Override
    public void onVideo(byte[] video, boolean isKeyFrame) {
        if(packetListener == null || !isHeaderWrite) {
            return;
        }
        int packetType = INTER_FRAME;
        if(isKeyFrame) {
            isKeyFrameWrite = true;
            packetType = KEY_FRAME;
        }
        //确保第一帧是关键帧，避免一开始出现灰色模糊界面
        if(!isKeyFrameWrite) {
            return;
        }
        int size = VIDEO_HEADER_SIZE + video.length;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeH264Packet(buffer, video, isKeyFrame);
        packetListener.onPacket(buffer.array(), packetType);
    }

    @Override
    public void onSpsPps(byte[] sps, byte[] pps) {
        if(packetListener == null) {
            return;
        }
        //写入第一个视频信息
        writeFirstVideoTag(sps, pps);
        //写入第一个音频信息
        writeFirstAudioTag();
        isHeaderWrite = true;
    }

    private void writeFirstVideoTag(byte[] sps, byte[] pps) {
        int size = VIDEO_HEADER_SIZE + VIDEO_SPECIFIC_CONFIG_EXTEND_SIZE + sps.length + pps.length;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeFirstVideoTag(buffer, sps, pps);
        packetListener.onPacket(buffer.array(), FIRST_VIDEO);
    }

    private void writeFirstAudioTag() {
        int size = AUDIO_SPECIFIC_CONFIG_SIZE + AUDIO_HEADER_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeFirstAudioTag(buffer, mAudioSampleRate, mIsStereo, mAudioSampleSize);
        packetListener.onPacket(buffer.array(), FIRST_AUDIO);
    }

    public void initAudioParams(int sampleRate, int sampleSize, boolean isStereo) {
        mAudioSampleRate = sampleRate;
        mAudioSampleSize = sampleSize;
        mIsStereo = isStereo;
    }
}

