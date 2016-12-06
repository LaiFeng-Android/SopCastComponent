package com.laifeng.sopcastsdk.stream.packer.flv;

import android.annotation.TargetApi;
import android.media.MediaCodec;

import com.laifeng.sopcastsdk.stream.packer.AnnexbHelper;
import com.laifeng.sopcastsdk.stream.packer.Packer;

import java.nio.ByteBuffer;

import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.AUDIO_HEADER_SIZE;
import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.AUDIO_SPECIFIC_CONFIG_SIZE;
import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.FLV_HEAD_SIZE;
import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.FLV_TAG_HEADER_SIZE;
import static com.laifeng.sopcastsdk.stream.packer.flv.FlvPackerHelper.PRE_SIZE;
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
@TargetApi(18)
public class FlvPacker implements Packer, AnnexbHelper.AnnexbNaluListener{

    public static final int HEADER = 0;
    public static final int METADATA = 1;
    public static final int FIRST_VIDEO = 2;
    public static final int FIRST_AUDIO = 3;
    public static final int AUDIO = 4;
    public static final int KEY_FRAME = 5;
    public static final int INTER_FRAME = 6;

    private OnPacketListener packetListener;
    private boolean isHeaderWrite;
    private boolean isKeyFrameWrite;

    private long mStartTime;
    private int mVideoWidth, mVideoHeight, mVideoFps;
    private int mAudioSampleRate, mAudioSampleSize;
    private boolean mIsStereo;

    private AnnexbHelper mAnnexbHelper;

    public FlvPacker() {
        mAnnexbHelper = new AnnexbHelper();
    }

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

        int compositionTime = (int) (System.currentTimeMillis() - mStartTime);
        int audioPacketSize = AUDIO_HEADER_SIZE + audio.length;
        int dataSize = audioPacketSize + FLV_TAG_HEADER_SIZE;
        int size = dataSize + PRE_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeFlvTagHeader(buffer, FlvPackerHelper.FlvTag.Audio, audioPacketSize, compositionTime);
        FlvPackerHelper.writeAudioTag(buffer, audio, false, mAudioSampleSize);
        buffer.putInt(dataSize);
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
        int compositionTime = (int) (System.currentTimeMillis() - mStartTime);
        int packetType = INTER_FRAME;
        if(isKeyFrame) {
            isKeyFrameWrite = true;
            packetType = KEY_FRAME;
        }
        //确保第一帧是关键帧，避免一开始出现灰色模糊界面
        if(!isKeyFrameWrite) {
            return;
        }

        int videoPacketSize = VIDEO_HEADER_SIZE + video.length;
        int dataSize = videoPacketSize + FLV_TAG_HEADER_SIZE;
        int size = dataSize + PRE_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeFlvTagHeader(buffer, FlvPackerHelper.FlvTag.Video, videoPacketSize, compositionTime);
        FlvPackerHelper.writeH264Packet(buffer, video, isKeyFrame);
        buffer.putInt(dataSize);
        packetListener.onPacket(buffer.array(), packetType);
    }

    @Override
    public void onSpsPps(byte[] sps, byte[] pps) {
        if(packetListener == null) {
            return;
        }
        //写入Flv header信息
        writeFlvHeader();
        //写入Meta 相关信息
        writeMetaData();
        //写入第一个视频信息
        writeFirstVideoTag(sps, pps);
        //写入第一个音频信息
        writeFirstAudioTag();
        mStartTime = System.currentTimeMillis();
        isHeaderWrite = true;
    }

    private void writeFlvHeader() {
        int size = FLV_HEAD_SIZE + PRE_SIZE;
        ByteBuffer headerBuffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeFlvHeader(headerBuffer, true, true);
        headerBuffer.putInt(0);
        packetListener.onPacket(headerBuffer.array(), HEADER);
    }

    private void writeMetaData() {
        byte[] metaData = FlvPackerHelper.writeFlvMetaData(mVideoWidth, mVideoHeight,
                mVideoFps, mAudioSampleRate, mAudioSampleSize, mIsStereo);
        int dataSize = metaData.length + FLV_TAG_HEADER_SIZE;
        int size = dataSize + PRE_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeFlvTagHeader(buffer, FlvPackerHelper.FlvTag.Script, metaData.length, 0);
        buffer.put(metaData);
        buffer.putInt(dataSize);
        packetListener.onPacket(buffer.array(), METADATA);
    }

    private void writeFirstVideoTag(byte[] sps, byte[] pps) {
        int firstPacketSize = VIDEO_HEADER_SIZE + VIDEO_SPECIFIC_CONFIG_EXTEND_SIZE + sps.length + pps.length;
        int dataSize = firstPacketSize + FLV_TAG_HEADER_SIZE;
        int size = dataSize + PRE_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeFlvTagHeader(buffer, FlvPackerHelper.FlvTag.Video, firstPacketSize, 0);
        FlvPackerHelper.writeFirstVideoTag(buffer, sps, pps);
        buffer.putInt(dataSize);
        packetListener.onPacket(buffer.array(), FIRST_VIDEO);
    }

    private void writeFirstAudioTag() {
        int firstAudioPacketSize = AUDIO_SPECIFIC_CONFIG_SIZE + AUDIO_HEADER_SIZE;
        int dataSize = FLV_TAG_HEADER_SIZE + firstAudioPacketSize;
        int size = dataSize + PRE_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        FlvPackerHelper.writeFlvTagHeader(buffer, FlvPackerHelper.FlvTag.Audio, firstAudioPacketSize, 0);
        FlvPackerHelper.writeFirstAudioTag(buffer, mAudioSampleRate, mIsStereo, mAudioSampleSize);
        buffer.putInt(dataSize);
        packetListener.onPacket(buffer.array(), FIRST_AUDIO);
    }

    public void initAudioParams(int sampleRate, int sampleSize, boolean isStereo) {
        mAudioSampleRate = sampleRate;
        mAudioSampleSize = sampleSize;
        mIsStereo = isStereo;
    }

    public void initVideoParams(int width, int height, int fps) {
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoFps = fps;
    }
}

