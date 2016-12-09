package com.laifeng.sopcastsdk.stream.packer;

import android.media.MediaCodec;

import com.laifeng.sopcastsdk.constant.SopCastConstant;
import com.laifeng.sopcastsdk.utils.SopCastLog;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * @Title: AnnexbHelper
 * @Package com.laifeng.sopcastsdk.video
 * @Description:
 * @Author Jim
 * @Date 16/9/1
 * @Time 下午2:20
 * @Version
 */
public class AnnexbHelper {

    // Coded slice of a non-IDR picture slice_layer_without_partitioning_rbsp( )
    public final static int NonIDR = 1;
    // Coded slice of an IDR picture slice_layer_without_partitioning_rbsp( )
    public final static int IDR = 5;
    // Supplemental enhancement information (SEI) sei_rbsp( )
    public final static int SEI = 6;
    // Sequence parameter set seq_parameter_set_rbsp( )
    public final static int SPS = 7;
    // Picture parameter set pic_parameter_set_rbsp( )
    public final static int PPS = 8;
    // Access unit delimiter access_unit_delimiter_rbsp( )
    public final static int AccessUnitDelimiter = 9;

    private AnnexbNaluListener mListener;

    private byte[] mPps;
    private byte[] mSps;
    private boolean mUploadPpsSps = true;



    /**
     * the search result for annexb.
     */
    class AnnexbSearch {
        public int startCode = 0;
        public boolean match = false;
    }

    public interface AnnexbNaluListener {
        void onSpsPps(byte[] sps, byte[] pps);
        void onVideo(byte[] data, boolean isKeyFrame);
    }

    public void setAnnexbNaluListener(AnnexbNaluListener listener) {
        mListener = listener;
    }


    public void stop() {
        mListener = null;
        mPps = null;
        mSps = null;
        mUploadPpsSps = true;
    }

    /**
     * 将硬编得到的视频数据进行处理生成每一帧视频数据，然后传给flv打包器
     * @param bb 硬编后的数据buffer
     * @param bi 硬编的BufferInfo
     */
    public void analyseVideoData(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        bb.position(bi.offset);
        bb.limit(bi.offset + bi.size);

        ArrayList<byte[]> frames = new ArrayList<>();
        boolean isKeyFrame = false;

        while(bb.position() < bi.offset + bi.size) {
            byte[] frame = annexbDemux(bb, bi);
            if(frame == null) {
                SopCastLog.e(SopCastConstant.TAG, "annexb not match.");
                break;
            }
            // ignore the nalu type aud(9)
            if (isAccessUnitDelimiter(frame)) {
                continue;
            }
            // for pps
            if(isPps(frame)) {
                mPps = frame;
                continue;
            }
            // for sps
            if(isSps(frame)) {
                mSps = frame;
                continue;
            }
            // for IDR frame
            if(isKeyFrame(frame)) {
                isKeyFrame = true;
            } else {
                isKeyFrame = false;
            }
            byte[] naluHeader = buildNaluHeader(frame.length);
            frames.add(naluHeader);
            frames.add(frame);
        }
        if (mPps != null && mSps != null && mListener != null && mUploadPpsSps) {
            if(mListener != null) {
                mListener.onSpsPps(mSps, mPps);
            }
            mUploadPpsSps = false;
        }
        if(frames.size() == 0 || mListener == null) {
            return;
        }
        int size = 0;
        for (int i = 0; i < frames.size(); i++) {
            byte[] frame = frames.get(i);
            size += frame.length;
        }
        byte[] data = new byte[size];
        int currentSize = 0;
        for (int i = 0; i < frames.size(); i++) {
            byte[] frame = frames.get(i);
            System.arraycopy(frame, 0, data, currentSize, frame.length);
            currentSize += frame.length;
        }
        if(mListener != null) {
            mListener.onVideo(data, isKeyFrame);
        }
    }

    /**
     * 从硬编出来的数据取出一帧nal
     * @param bb
     * @param bi
     * @return
     */
    private byte[] annexbDemux(ByteBuffer bb, MediaCodec.BufferInfo bi) {
        AnnexbSearch annexbSearch = new AnnexbSearch();
        avcStartWithAnnexb(annexbSearch, bb, bi);

        if (!annexbSearch.match || annexbSearch.startCode < 3) {
            return null;
        }

        for (int i = 0; i < annexbSearch.startCode; i++) {
            bb.get();
        }

        ByteBuffer frameBuffer = bb.slice();
        int pos = bb.position();
        while (bb.position() < bi.offset + bi.size) {
            avcStartWithAnnexb(annexbSearch, bb, bi);
            if (annexbSearch.match) {
                break;
            }
            bb.get();
        }

        int size = bb.position() - pos;
        byte[] frameBytes = new byte[size];
        frameBuffer.get(frameBytes);
        return frameBytes;
    }


    /**
     * 从硬编出来的byteBuffer中查找nal
     * @param as
     * @param bb
     * @param bi
     */
    private void avcStartWithAnnexb(AnnexbSearch as, ByteBuffer bb, MediaCodec.BufferInfo bi) {
        as.match = false;
        as.startCode = 0;
        int pos = bb.position();
        while (pos < bi.offset + bi.size - 3) {
            // not match.
            if (bb.get(pos) != 0x00 || bb.get(pos + 1) != 0x00) {
                break;
            }

            // match N[00] 00 00 01, where N>=0
            if (bb.get(pos + 2) == 0x01) {
                as.match = true;
                as.startCode = pos + 3 - bb.position();
                break;
            }
            pos++;
        }
    }

    private byte[] buildNaluHeader(int length) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(length);
        return buffer.array();
    }


    private boolean isSps(byte[] frame) {
        if (frame.length < 1) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[0] & 0x1f);
        return nal_unit_type == SPS;
    }

    private boolean isPps(byte[] frame) {
        if (frame.length < 1) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[0] & 0x1f);
        return nal_unit_type == PPS;
    }

    private boolean isKeyFrame(byte[] frame) {
        if (frame.length < 1) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[0] & 0x1f);
        return nal_unit_type == IDR;
    }

    private static boolean isAccessUnitDelimiter(byte[] frame) {
        if (frame.length < 1) {
            return false;
        }
        // 5bits, 7.3.1 NAL unit syntax,
        // H.264-AVC-ISO_IEC_14496-10.pdf, page 44.
        //  7: SPS, 8: PPS, 5: I Frame, 1: P Frame
        int nal_unit_type = (frame[0] & 0x1f);
        return nal_unit_type == AccessUnitDelimiter;
    }
}
