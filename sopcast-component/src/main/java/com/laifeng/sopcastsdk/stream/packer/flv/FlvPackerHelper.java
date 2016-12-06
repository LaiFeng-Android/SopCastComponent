package com.laifeng.sopcastsdk.stream.packer.flv;

import com.laifeng.sopcastsdk.stream.amf.AmfMap;
import com.laifeng.sopcastsdk.stream.amf.AmfString;

import java.nio.ByteBuffer;

/**
 * @Title: FlvPackerHelper
 * @Package com.laifeng.sopcastsdk.stream.packer.flv
 * @Description:
 * @Author Jim
 * @Date 16/9/23
 * @Time 下午6:34
 * @Version
 */

public class FlvPackerHelper {
    public static final int FLV_HEAD_SIZE = 9;
    public static final int VIDEO_HEADER_SIZE = 5;
    public static final int AUDIO_HEADER_SIZE = 2;
    public static final int FLV_TAG_HEADER_SIZE = 11;
    public static final int PRE_SIZE = 4;
    public static final int AUDIO_SPECIFIC_CONFIG_SIZE = 2;
    public static final int VIDEO_SPECIFIC_CONFIG_EXTEND_SIZE = 11;

    /**
     * 生成flv 头信息
     * @param buffer 需要写入数据的byte buffer
     * @param hasVideo 是否有视频
     * @param hasAudio 是否有音频
     * @return byte数组
     */
    public static void writeFlvHeader(ByteBuffer buffer, boolean hasVideo, boolean hasAudio) {
        /**
         *  Flv Header在当前版本中总是由9个字节组成。
         *  第1-3字节为文件标识（Signature），总为“FLV”（0x46 0x4C 0x56），如图中紫色区域。
         *  第4字节为版本，目前为1（0x01）。
         *  第5个字节的前5位保留，必须为0。
         *  第5个字节的第6位表示是否存在音频Tag。
         *  第5个字节的第7位保留，必须为0。
         *  第5个字节的第8位表示是否存在视频Tag。
         *  第6-9个字节为UI32类型的值，表示从File Header开始到File Body开始的字节数，版本1中总为9。
         */
        byte[] signature = new byte[] {'F', 'L', 'V'};  /* always "FLV" */
        byte version = (byte) 0x01;     /* should be 1 */
        byte videoFlag = hasVideo ? (byte) 0x01 : 0x00;
        byte audioFlag = hasAudio ? (byte) 0x04 : 0x00;
        byte flags = (byte) (videoFlag | audioFlag);  /* 4, audio; 1, video; 5 audio+video.*/
        byte[] offset = new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09};  /* always 9 */

        buffer.put(signature);
        buffer.put(version);
        buffer.put(flags);
        buffer.put(offset);
    }

    /**
     * 写flv tag头信息
     * @param buffer 需要写入数据的byte buffer
     * @param type 类型：音频（0x8），视频（0x9），脚本（0x12）
     * @param dataSize 数据大小
     * @param timestamp 时间戳
     * @return byte数组
     */
    public static void writeFlvTagHeader(ByteBuffer buffer, int type, int dataSize, int timestamp) {
        /**
         * 第1个byte为记录着tag的类型，音频（0x8），视频（0x9），脚本（0x12）；
         * 第2-4bytes是数据区的长度，UI24类型的值，也就是tag data的长度；注：这个长度等于最后的Tag Size-11
         * 第5-7个bytes是时间戳，UI24类型的值，单位是毫秒，类型为0x12脚本类型数据，则时间戳为0，时间戳控制着文件播放的速度，可以根据音视频的帧率类设置；
         * 第8个byte是扩展时间戳，当24位数值不够时，该字节作为最高位将时间戳扩展为32位值；
         * 第9-11个bytes是streamID，UI24类型的值，但是总为0；
         * tag header 长度为1+3+3+1+3=11。
         */
        int sizeAndType = (dataSize & 0x00FFFFFF) | ((type & 0x1F) << 24);
        buffer.putInt(sizeAndType);
        int time = ((timestamp << 8) & 0xFFFFFF00) | ((timestamp >> 24) & 0x000000FF);
        buffer.putInt(time);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
    }

    /**
     * 生成flv medadata 数据
     * @param width 视频宽度
     * @param height 视频高度
     * @param fps 视频帧率
     * @param audioRate 音频采样率
     * @param audioSize 音频大小
     * @param isStereo 音频是否为立体声
     * @return byte数组
     */
    public static byte[] writeFlvMetaData(int width, int height, int fps, int audioRate, int audioSize, boolean isStereo) {
        AmfString metaDataHeader = new AmfString("onMetaData", false);
        AmfMap amfMap = new AmfMap();
        amfMap.setProperty("width", width);
        amfMap.setProperty("height", height);
        amfMap.setProperty("framerate", fps);
        amfMap.setProperty("videocodecid", FlvVideoCodecID.AVC);
        amfMap.setProperty("audiosamplerate", audioRate);
        amfMap.setProperty("audiosamplesize", audioSize);
        if(isStereo) {
            amfMap.setProperty("stereo", true);
        } else {
            amfMap.setProperty("stereo", false);
        }
        amfMap.setProperty("audiocodecid", FlvAudio.AAC);

        int size = amfMap.getSize() + metaDataHeader.getSize();
        ByteBuffer amfBuffer = ByteBuffer.allocate(size);
        amfBuffer.put(metaDataHeader.getBytes());
        amfBuffer.put(amfMap.getBytes());
        return amfBuffer.array();
    }


    /**
     * 第一个视频Tag，需要写入AVC视频流的configuretion信息，这个信息根据pps、sps生成
     * 8 bit configuration version ------ 版本号
     * 8 bit AVC Profile Indication ------- sps[1]
     * 8 bit Profile Compatibility ------- sps[2]
     * 8 bit AVC Level Compatibility ------- sps[3]
     * 6 bit Reserved ------- 111111
     * 2 bit Length Size Minus One ------- NAL Unit Length长度为－1，一般为3
     * 3 bit Reserved ------- 111
     * 5 bit Num of Sequence Parameter Sets ------- sps个数，一般为1
     * ? bit Sequence Parameter Set NAL Units ------- （sps_size + sps）的数组
     * 8 bit Num of Picture Parameter Sets ------- pps个数，一般为1
     * ? bit Picture Parameter Set NAL Units ------- （pps_size + pps）的数组
     * @param sps
     * @param pps
     * @return
     */
    public static void writeFirstVideoTag(ByteBuffer buffer, byte[] sps, byte[] pps) {
        //写入Flv Video Header
        writeVideoHeader(buffer, FlvVideoFrameType.KeyFrame, FlvVideoCodecID.AVC, FlvVideoAVCPacketType.SequenceHeader);

        buffer.put((byte)0x01);
        buffer.put(sps[1]);
        buffer.put(sps[2]);
        buffer.put(sps[3]);
        buffer.put((byte)0xff);

        buffer.put((byte)0xe1);
        buffer.putShort((short)sps.length);
        buffer.put(sps);

        buffer.put((byte)0x01);
        buffer.putShort((short)pps.length);
        buffer.put(pps);
    }

    /**
     * 封装flv 视频头信息
     * 4 bit Frame Type  ------ 帧类型
     * 4 bit CodecID ------ 视频类型
     * 8 bit AVCPacketType ------ 是NALU 还是 sequence header
     * 24 bit CompositionTime ------ 如果为NALU 则为时间间隔，否则为0
     * @param flvVideoFrameType 参见 class FlvVideoFrameType
     * @param codecID 参见 class FlvVideo
     * @param AVCPacketType 参见 class FlvVideoAVCPacketType
     * @return
     */
    public static void writeVideoHeader(ByteBuffer buffer, int flvVideoFrameType, int codecID, int AVCPacketType) {
        byte first = (byte) (((flvVideoFrameType & 0x0F) << 4)| (codecID & 0x0F));
        buffer.put(first);

        buffer.put((byte) AVCPacketType);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
    }

    /**
     * 写视频tag
     * @param data 视频数据
     * @param isKeyFrame 是否为关键帧
     * @return byte数组
     */
    public static void writeH264Packet(ByteBuffer buffer, byte[] data, boolean isKeyFrame) {
        //生成Flv Video Header
        int flvVideoFrameType = FlvVideoFrameType.InterFrame;
        if(isKeyFrame) {
            flvVideoFrameType = FlvVideoFrameType.KeyFrame;
        }
        writeVideoHeader(buffer, flvVideoFrameType, FlvVideoCodecID.AVC, FlvVideoAVCPacketType.NALU);

        //写入视频信息
        buffer.put(data);
    }

    /**
     * 写第一个音频tag
     * @param audioRate 音频采样率
     * @param isStereo 是否为立体声
     * @return byte数组
     */
    public static void writeFirstAudioTag(ByteBuffer buffer, int audioRate, boolean isStereo, int audioSize) {
        byte[] audioInfo = new byte[2];
        int soundRateIndex = getAudioSimpleRateIndex(audioRate);
        int channelCount = 1;
        if(isStereo) {
            channelCount = 2;
        }
        audioInfo[0] = (byte) (0x10 | ((soundRateIndex>>1) & 0x7));
        audioInfo[1] = (byte) (((soundRateIndex & 0x1)<<7) | ((channelCount & 0xF) << 3));
        writeAudioTag(buffer, audioInfo, true, audioSize);
    }


    public static void writeAudioTag(ByteBuffer buffer, byte[] audioInfo, boolean isFirst, int audioSize) {
        //写入音频头信息
        writeAudioHeader(buffer, isFirst, audioSize);

        //写入音频信息
        buffer.put(audioInfo);
    }

    /**
     * 写Audio Header信息
     * soundFormat 声音类型 参见 FlvAudio
     * soundRate 声音采样频率 参加 FlvAudioSampleRate
     * soundSize 声音采样大小 参加 FlvAudioSampleSize
     * soundType 声音的类别 参加 FlvAudioSampleType
     * AACPacketType 0 ＝ AAC sequence header   1 = AAC raw
     * @return
     */
    public static void writeAudioHeader(ByteBuffer buffer, boolean isFirst, int audioSize) {
        int soundFormat = FlvAudio.AAC;

        // AAC always 3
        int soundRateIndex = 3;

        int soundSize = FlvAudioSampleSize.PCM_16;
        if(audioSize == 8) {
            soundSize = FlvAudioSampleSize.PCM_8;
        }

        // aac always stereo
        int soundType = FlvAudioSampleType.STEREO;

        int AACPacketType = FlvAudioAACPacketType.Raw;
        if(isFirst) {
            AACPacketType = FlvAudioAACPacketType.SequenceHeader;
        }

        byte[] header = new byte[2];
        header[0] = (byte)(((byte) (soundFormat & 0x0F) << 4) | ((byte) (soundRateIndex & 0x03) << 2) | ((byte) (soundSize & 0x01) << 1) | ((byte) (soundType & 0x01)));
        header[1] = (byte) AACPacketType;
        buffer.put(header);
    }


    /**
     * 根据传入的采样频率获取规定的采样频率的index
     * @param audioSampleRate
     * @return
     */
    public static int getAudioSimpleRateIndex(int audioSampleRate) {
        int simpleRateIndex;
        switch (audioSampleRate) {
            case 96000:
                simpleRateIndex = 0;
                break;
            case 88200:
                simpleRateIndex = 1;
                break;
            case 64000:
                simpleRateIndex = 2;
                break;
            case 48000:
                simpleRateIndex = 3;
                break;
            case 44100:
                simpleRateIndex = 4;
                break;
            case 32000:
                simpleRateIndex = 5;
                break;
            case 24000:
                simpleRateIndex = 6;
                break;
            case 22050:
                simpleRateIndex = 7;
                break;
            case 16000:
                simpleRateIndex = 8;
                break;
            case 12000:
                simpleRateIndex = 9;
                break;
            case 11025:
                simpleRateIndex = 10;
                break;
            case 8000:
                simpleRateIndex = 11;
                break;
            case 7350:
                simpleRateIndex = 12;
                break;
            default:
                simpleRateIndex = 15;
        }
        return simpleRateIndex;
    }

    /**
     * E.4.3.1 VIDEODATA
     * Frame Type UB [4]
     * Type of video frame. The following values are defined:
     *  1 = key frame (for AVC, a seekable frame)
     *  2 = inter frame (for AVC, a non-seekable frame)
     *  3 = disposable inter frame (H.263 only)
     *  4 = generated key frame (reserved for server use only)
     *  5 = video info/command frame
     */
    public class FlvVideoFrameType
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved = 0;
        public final static int Reserved1 = 6;

        public final static int KeyFrame                     = 1;
        public final static int InterFrame                 = 2;
        public final static int DisposableInterFrame         = 3;
        public final static int GeneratedKeyFrame            = 4;
        public final static int VideoInfoFrame                = 5;
    }


    /**
     * AVCPacketType IF CodecID == 7 UI8
     * The following values are defined:
     *  0 = AVC sequence header
     *  1 = AVC NALU
     *  2 = AVC end of sequence (lower level NALU sequence ender is not required or supported)
     */
    public class FlvVideoAVCPacketType
    {
        // set to the max value to reserved, for array map.
        public final static int Reserved                    = 3;

        public final static int SequenceHeader                 = 0;
        public final static int NALU                         = 1;
        public final static int SequenceHeaderEOF             = 2;
    }

    /**
     * AACPacketType
     * The following values are defined:
     *  0 = AAC sequence header
     *  1 = AAC Raw
     */
    public class FlvAudioAACPacketType
    {
        public final static int SequenceHeader                 = 0;
        public final static int Raw                         = 1;
    }

    /**
     * E.4.1 FLV Tag, page 75
     */
    public class FlvTag
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved = 0;
        // 8 = audio
        public final static int Audio = 8;
        // 9 = video
        public final static int Video = 9;
        // 18 = script data
        public final static int Script = 18;
    }

    /**
     * E.4.3.1 VIDEODATA
     * CodecID UB [4]
     * Codec Identifier. The following values are defined:
     *  2 = Sorenson H.263
     *  3 = Screen video
     *  4 = On2 VP6
     *  5 = On2 VP6 with alpha channel
     *  6 = Screen video version 2
     *  7 = AVC
     */
    public class FlvVideoCodecID
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved                = 0;
        public final static int Reserved1                = 1;
        public final static int Reserved2                = 9;

        // for user to disable video, for example, use pure audio hls.
        public final static int Disabled                = 8;

        public final static int SorensonH263             = 2;
        public final static int ScreenVideo             = 3;
        public final static int On2VP6                 = 4;
        public final static int On2VP6WithAlphaChannel = 5;
        public final static int ScreenVideoVersion2     = 6;
        public final static int AVC                     = 7;
    }

    public class FlvAudio {
        public final static int LINEAR_PCM = 0;
        public final static int AD_PCM = 1;
        public final static int MP3 = 2;
        public final static int LINEAR_PCM_LE = 3;
        public final static int NELLYMOSER_16_MONO = 4;
        public final static int NELLYMOSER_8_MONO = 5;
        public final static int NELLYMOSER = 6;
        public final static int G711_A = 7;
        public final static int G711_MU = 8;
        public final static int RESERVED = 9;
        public final static int AAC = 10;
        public final static int SPEEX = 11;
        public final static int MP3_8 = 14;
        public final static int DEVICE_SPECIFIC = 15;
    }

    /**
     * the aac object type, for RTMP sequence header
     * for AudioSpecificConfig, @see aac-mp4a-format-ISO_IEC_14496-3+2001.pdf, page 33
     * for audioObjectType, @see aac-mp4a-format-ISO_IEC_14496-3+2001.pdf, page 23
     */
    public class FlvAacObjectType
    {
        public final static int Reserved = 0;

        // Table 1.1 – Audio Object Type definition
        // @see @see aac-mp4a-format-ISO_IEC_14496-3+2001.pdf, page 23
        public final static int AacMain = 1;
        public final static int AacLC = 2;
        public final static int AacSSR = 3;

        // AAC HE = LC+SBR
        public final static int AacHE = 5;
        // AAC HEv2 = LC+SBR+PS
        public final static int AacHEV2 = 29;
    }

    /**
     * the aac profile, for ADTS(HLS/TS)
     * @see "https://github.com/simple-rtmp-server/srs/issues/310"
     */
    public class FlvAacProfile
    {
        public final static int Reserved = 3;

        // @see 7.1 Profiles, aac-iso-13818-7.pdf, page 40
        public final static int Main = 0;
        public final static int LC = 1;
        public final static int SSR = 2;
    }

    /**
     * the FLV/RTMP supported audio sample rate.
     * Sampling rate. The following values are defined:
     */
    public class FlvAudioSampleRate
    {
        // set to the max value to reserved, for array map.
        public final static int Reserved                 = 15;

        public final static int R96000                     = 0;
        public final static int R88200                    = 1;
        public final static int R64000                    = 2;
        public final static int R48000                    = 3;
        public final static int R44100                    = 4;
        public final static int R32000                    = 5;
        public final static int R24000                    = 6;
        public final static int R22050                    = 7;
        public final static int R16000                    = 8;
        public final static int R12000                    = 9;
        public final static int R11025                    = 10;
        public final static int R8000                    = 11;
        public final static int R7350                    = 12;
    }

    /**
     * the FLV/RTMP supported audio sample size.
     * Sampling size. The following values are defined:
     * 0 = 8-bit samples
     * 1 = 16-bit samples
     */
    public class FlvAudioSampleSize
    {
        public final static int PCM_8                     = 0;
        public final static int PCM_16                    = 1;
    }

    /**
     * the FLV/RTMP supported audio sample type.
     * Sampling type. The following values are defined:
     * 0 = Mono sound
     * 1 = Stereo sound
     */
    public class FlvAudioSampleType
    {
        public final static int MONO                     = 0;
        public final static int STEREO                   = 1;
    }

    /**
     * the type of message to process.
     */
    public class FlvMessageType {
        public final static int FLV = 0x100;
    }

    /**
     * Table 7-1 – NAL unit type codes, syntax element categories, and NAL unit type classes
     * H.264-AVC-ISO_IEC_14496-10-2012.pdf, page 83.
     */
    public class FlvAvcNaluType
    {
        // Unspecified
        public final static int Reserved = 0;

        // Coded slice of a non-IDR picture slice_layer_without_partitioning_rbsp( )
        public final static int NonIDR = 1;
        // Coded slice data partition A slice_data_partition_a_layer_rbsp( )
        public final static int DataPartitionA = 2;
        // Coded slice data partition B slice_data_partition_b_layer_rbsp( )
        public final static int DataPartitionB = 3;
        // Coded slice data partition C slice_data_partition_c_layer_rbsp( )
        public final static int DataPartitionC = 4;
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
        // End of sequence end_of_seq_rbsp( )
        public final static int EOSequence = 10;
        // End of stream end_of_stream_rbsp( )
        public final static int EOStream = 11;
        // Filler data filler_data_rbsp( )
        public final static int FilterData = 12;
        // Sequence parameter set extension seq_parameter_set_extension_rbsp( )
        public final static int SPSExt = 13;
        // Prefix NAL unit prefix_nal_unit_rbsp( )
        public final static int PrefixNALU = 14;
        // Subset sequence parameter set subset_seq_parameter_set_rbsp( )
        public final static int SubsetSPS = 15;
        // Coded slice of an auxiliary coded picture without partitioning slice_layer_without_partitioning_rbsp( )
        public final static int LayerWithoutPartition = 19;
        // Coded slice extension slice_layer_extension_rbsp( )
        public final static int CodedSliceExt = 20;
    }


    /**
     * 0 = Number type  //DOUBLE(8个字节的double数据)
     * 1 = Boolean type //UI8(1个字节)
     * 2 = String type   //SCRIPTDATASTRING
     * 3 = Object type  //SCRIPTDATAOBJECT[n]
     * 4 = MovieClip type  //SCRIPTDATASTRING
     * 5 = Null type
     * 6 = Undefined type
     * 7 = Reference type  //UI16(2个字节)
     * 8 = ECMA array type  //SCRIPTDATAVARIABLE[ECMAArrayLength]
     * 10 = Strict array type  //SCRIPTDATAVARIABLE[n]
     * 11 = Date type  //SCRIPTDATADATE
     * 12 = Long string type  //SCRIPTDATALONGSTRING
     */
    public class FlvMetaValueType {
        public final static int NumberType = 0;
        public final static int BooleanType = 1;
        public final static int StringType = 2;
        public final static int ObjectType = 3;
        public final static int MovieClipType = 4;
        public final static int NullType = 5;
        public final static int UndefinedType = 6;
        public final static int ReferenceType = 7;
        public final static int ECMAArrayType = 8;
        public final static int StrictArrayType = 10;
        public final static int DateType = 11;
        public final static int LongStringType = 12;
    }
}
