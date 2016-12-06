package com.laifeng.sopcastsdk.stream.sender.rtmp.io;

import android.util.Log;

import com.laifeng.sopcastsdk.entity.Frame;
import com.laifeng.sopcastsdk.stream.amf.AmfMap;
import com.laifeng.sopcastsdk.stream.amf.AmfNull;
import com.laifeng.sopcastsdk.stream.amf.AmfNumber;
import com.laifeng.sopcastsdk.stream.amf.AmfObject;
import com.laifeng.sopcastsdk.stream.amf.AmfString;
import com.laifeng.sopcastsdk.stream.packer.rtmp.RtmpPacker;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Abort;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Audio;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.ChunkHeader;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Command;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Data;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Handshake;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Chunk;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.MessageType;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.UserControl;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.Video;
import com.laifeng.sopcastsdk.stream.sender.rtmp.packets.WindowAckSize;
import com.laifeng.sopcastsdk.stream.sender.sendqueue.ISendQueue;
import com.laifeng.sopcastsdk.stream.sender.sendqueue.NormalSendQueue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main RTMP connection implementation class
 * 
 * @author francois, leoma
 */
public class RtmpConnection implements OnReadListener, OnWriteListener {

    private static final String TAG = "RtmpConnection";
    private static final Pattern rtmpUrlPattern = Pattern.compile("^rtmp://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");

    private RtmpConnectListener listener;
    private Socket socket;
    private SessionInfo sessionInfo;
    private ReadThread readThread;
    private WriteThread writeThread;
    private State state = State.INIT;
    private int transactionIdCounter = 0;
    private int currentStreamId = -1;
    private ConnectData connectData;

    private int videoWidth, videoHeight;
    private int audioSampleRate, audioSampleSize;
    private boolean isAudioStereo;

    private boolean publishPermitted;
    private ISendQueue mSendQueue;

    public enum State {
        INIT,
        HANDSHAKE,
        CONNECTING,
        CREATE_STREAM,
        PUBLISHING,
        LIVING
    }

    public static class ConnectData {
        public String appName;
        public String streamName;
        public String swfUrl;
        public String tcUrl;
        public String pageUrl;
        public int port;
        public String host;
    }

    public void setConnectListener(RtmpConnectListener listener) {
        this.listener = listener;
    }

    public void setSendQueue(ISendQueue sendQueue) {
        mSendQueue = sendQueue;
    }

    public void connect(String url) {
        state = State.INIT;
        connectData = parseRtmpUrl(url);
        if(connectData == null) {
            if(listener != null) {
                listener.onUrlInvalid();
            }
            return;
        }
        String host = connectData.host;
        int port = connectData.port;
        String appName = connectData.appName;
        String streamName = connectData.streamName;
        Log.d(TAG, "connect() called. Host: " + host + ", port: " + port + ", appName: " + appName + ", publishPath: " + streamName);
        socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        try {
            socket.connect(socketAddress, 3000);
        } catch (IOException e) {
            e.printStackTrace();
            if(listener != null) {
                listener.onSocketConnectFail();
            }
            return;
        }
        if(listener != null) {
            listener.onSocketConnectSuccess();
        }
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        state = State.HANDSHAKE;
        try {
            Log.d(TAG, "connect(): socket connection established, doing handhake...");
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());
            handshake(in, out);
        } catch (IOException e) {
            e.printStackTrace();
            state = State.INIT;
            clearSocket();
            if(listener != null) {
                listener.onHandshakeFail();
            }
            return;
        }
        if(listener != null) {
            listener.onHandshakeSuccess();
        }
        sessionInfo = new SessionInfo();
        readThread = new ReadThread(in, sessionInfo);
        writeThread = new WriteThread(out, sessionInfo);
        readThread.setOnReadListener(this);
        writeThread.setWriteListener(this);
        writeThread.setSendQueue(mSendQueue);
        readThread.start();
        writeThread.start();
        rtmpConnect();
    }

    private ConnectData parseRtmpUrl(String url) {
        ConnectData connectData = null;
        Matcher matcher = rtmpUrlPattern.matcher(url);
        if (matcher.matches()) {
            connectData = new ConnectData();
            connectData.tcUrl = url.substring(0, url.lastIndexOf('/'));
            connectData.swfUrl = "";
            connectData.pageUrl = "";
            connectData.host = matcher.group(1);
            String portStr = matcher.group(3);
            connectData.port = portStr != null ? Integer.parseInt(portStr) : 1935;
            connectData.appName = matcher.group(4);
            connectData.streamName = matcher.group(6);
        }
        return connectData;
    }

    private void handshake(InputStream in, OutputStream out) throws IOException {
        Handshake handshake = new Handshake();
        handshake.writeC0(out);
        handshake.writeC1(out); // Write C1 without waiting for S0
        out.flush();
        handshake.readS0(in);
        handshake.readS1(in);
        handshake.writeC2(out);
        handshake.readS2(in);
    }

    private void clearSocket() {
        if (socket != null && socket.isConnected()) {
            try {
                socket.close();
                socket = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void rtmpConnect() {
        SessionInfo.markSessionTimestampTx();
        Command invoke = new Command("connect", ++transactionIdCounter);

        AmfObject args = new AmfObject();
        args.setProperty("app", connectData.appName);
        args.setProperty("flashVer", "LNX 11,2,202,233"); // Flash player OS: Linux, version: 11.2.202.233
        args.setProperty("swfUrl", connectData.swfUrl);
        args.setProperty("tcUrl", connectData.tcUrl);
        args.setProperty("fpad", false);
        args.setProperty("capabilities", 239);
        args.setProperty("audioCodecs", 3575);
        args.setProperty("videoCodecs", 252);
        args.setProperty("videoFunction", 1);
        args.setProperty("pageUrl", connectData.pageUrl);
        args.setProperty("objectEncoding", 0);
        invoke.addData(args);
        Frame<Chunk> frame = new Frame(invoke, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
        mSendQueue.putFrame(frame);
        state = State.CONNECTING;
    }

    @Override
    public void onChunkRead(Chunk chunk) {
        ChunkHeader chunkHeader = chunk.getChunkHeader();
        MessageType messageType = chunkHeader.getMessageType();
        switch (messageType) {
            case ABORT:
                readThread.clearStoredChunks(((Abort) chunk).getChunkStreamId());
                break;
            case USER_CONTROL_MESSAGE:
                UserControl ping = (UserControl) chunk;
                if(ping.getType() == UserControl.Type.PING_REQUEST) {
                    Log.d(TAG, "Sending PONG reply..");
                    UserControl pong = new UserControl();
                    pong.setType(UserControl.Type.PONG_REPLY);
                    pong.setEventData(ping.getEventData()[0]);
                    Frame<Chunk> frame = new Frame(pong, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
                    mSendQueue.putFrame(frame);
                } else if(ping.getType() == UserControl.Type.STREAM_EOF) {
                    Log.d(TAG, "Stream EOF reached");
                }
                break;
            case WINDOW_ACKNOWLEDGEMENT_SIZE:
                WindowAckSize windowAckSize = (WindowAckSize) chunk;
                int size = windowAckSize.getAcknowledgementWindowSize();
                Log.d(TAG, "Setting acknowledgement window size: " + size);
                sessionInfo.setAcknowledgmentWindowSize(size);
                // Set socket option
                try {
                    socket.setSendBufferSize(size);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                break;
            case SET_PEER_BANDWIDTH:
                int acknowledgementWindowsize = sessionInfo.getAcknowledgementWindowSize();
                Log.d(TAG, "Send acknowledgement window size: " + acknowledgementWindowsize);
                Chunk setPeerBandwidth = new WindowAckSize(acknowledgementWindowsize);
                Frame<Chunk> frame = new Frame(setPeerBandwidth, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
                mSendQueue.putFrame(frame);
                break;
            case COMMAND_AMF0:
                handleRxCommandInvoke((Command) chunk);
                break;
            default:
                Log.w(TAG, "Not handling unimplemented/unknown packet of type: " + chunkHeader.getMessageType());
                break;
        }
    }

    private void handleRxCommandInvoke(Command command) {
        String commandName = command.getCommandName();
        if(commandName.equals("_result")) {
            String method = sessionInfo.takeInvokedCommand(command.getTransactionId());
            Log.d(TAG, "Got result for invoked method: " + method);
            if ("connect".equals(method)) {
                if(listener != null) {
                    listener.onRtmpConnectSuccess();
                }
                createStream();
            } else if("createStream".equals(method)) {
                currentStreamId = (int) ((AmfNumber) command.getData().get(1)).getValue();
                if(listener != null) {
                    listener.onCreateStreamSuccess();
                }
                fmlePublish();
            }
        } else if(commandName.equals("_error")) {
            String method = sessionInfo.takeInvokedCommand(command.getTransactionId());
            Log.d(TAG, "Got error for invoked method: " + method);
            if ("connect".equals(method)) {
                stop();
                if(listener != null) {
                    listener.onRtmpConnectFail();
                }
            } else if("createStream".equals(method)) {
                stop();
                if(listener != null) {
                    listener.onCreateStreamFail();
                }
            }
        } else if(commandName.equals("onStatus")) {
            String code = ((AmfString) ((AmfObject) command.getData().get(1)).getProperty("code")).getValue();
            if (code.equals("NetStream.Publish.Start")) {
                Log.d(TAG, "Got publish start success");
                state = State.LIVING;
                if(listener != null) {
                    listener.onPublishSuccess();
                }
                onMetaData();
                // We can now publish AV data
                publishPermitted = true;
            } else {
                Log.d(TAG, "Got publish start fail");
                stop();
                if(listener != null) {
                    listener.onPublishFail();
                }
            }
        } else {
            Log.d(TAG, "Got Command result: " + commandName);
        }
    }

    private void createStream() {
        state = State.CREATE_STREAM;
        Log.d(TAG, "createStream(): Sending releaseStream command...");
        // transactionId == 2
        Command releaseStream = new Command("releaseStream", ++transactionIdCounter);
        releaseStream.getChunkHeader().setChunkStreamId(SessionInfo.RTMP_STREAM_CHANNEL);
        releaseStream.addData(new AmfNull());  // command object: null for "createStream"
        releaseStream.addData(connectData.streamName);  // command object: null for "releaseStream"
        Frame<Chunk> frame1 = new Frame(releaseStream, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
        mSendQueue.putFrame(frame1);

        Log.d(TAG, "createStream(): Sending FCPublish command...");
        // transactionId == 3
        Command FCPublish = new Command("FCPublish", ++transactionIdCounter);
        FCPublish.getChunkHeader().setChunkStreamId(SessionInfo.RTMP_STREAM_CHANNEL);
        FCPublish.addData(new AmfNull());  // command object: null for "FCPublish"
        FCPublish.addData(connectData.streamName);
        Frame<Chunk> frame2 = new Frame(FCPublish, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
        mSendQueue.putFrame(frame2);

        Log.d(TAG, "createStream(): Sending createStream command...");
        // transactionId == 4
        Command createStream = new Command("createStream", ++transactionIdCounter);
        createStream.addData(new AmfNull());  // command object: null for "createStream"
        Frame<Chunk> frame3 = new Frame(createStream, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
        mSendQueue.putFrame(frame3);
    }

    private void fmlePublish() {
        if (currentStreamId == -1 || connectData == null) {
            return;
        }
        state = State.PUBLISHING;
        Log.d(TAG, "fmlePublish(): Sending publish command...");
        // transactionId == 0
        Command publish = new Command("publish", 0);
        publish.getChunkHeader().setChunkStreamId(SessionInfo.RTMP_STREAM_CHANNEL);
        publish.getChunkHeader().setMessageStreamId(currentStreamId);
        publish.addData(new AmfNull());  // command object: null for "publish"
        publish.addData(connectData.streamName);
        publish.addData("live");
        Frame<Chunk> frame = new Frame(publish, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
        mSendQueue.putFrame(frame);
    }

    private void onMetaData() {
        if (currentStreamId == -1) {
            return;
        }

        Log.d(TAG, "onMetaData(): Sending empty onMetaData...");
        Data metadata = new Data("@setDataFrame");
        metadata.getChunkHeader().setMessageStreamId(currentStreamId);
        metadata.addData("onMetaData");
        AmfMap ecmaArray = new AmfMap();
        ecmaArray.setProperty("duration", 0);
        ecmaArray.setProperty("width", videoWidth);
        ecmaArray.setProperty("height", videoHeight);
        ecmaArray.setProperty("videodatarate", 0);
        ecmaArray.setProperty("framerate", 0);
        ecmaArray.setProperty("audiodatarate", 0);
        ecmaArray.setProperty("audiosamplerate", audioSampleRate);
        ecmaArray.setProperty("audiosamplesize", audioSampleSize);
        ecmaArray.setProperty("stereo", isAudioStereo);
        ecmaArray.setProperty("filesize", 0);
        metadata.addData(ecmaArray);
        Frame<Chunk> frame = new Frame(metadata, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
        mSendQueue.putFrame(frame);
    }

    public void setVideoParams(int width, int height) {
        videoWidth = width;
        videoHeight = height;
    }

    public void setAudioParams(int sampleRate, int sampleSize, boolean isStereo) {
        audioSampleRate = sampleRate;
        audioSampleSize = sampleSize;
        isAudioStereo = isStereo;
    }

    public void publishAudioData(byte[] data, int type) {
        if (currentStreamId == -1) {
            return;
        }
        if (!publishPermitted) {
            return;
        }
        Audio audio = new Audio();
        audio.setData(data);
        audio.getChunkHeader().setMessageStreamId(currentStreamId);
        Frame<Chunk> frame;
        if(type == RtmpPacker.FIRST_AUDIO) {
            frame = new Frame(audio, type, Frame.FRAME_TYPE_CONFIGURATION);
        } else {
            frame = new Frame(audio, type, Frame.FRAME_TYPE_AUDIO);
        }
        mSendQueue.putFrame(frame);
    }

    public void publishVideoData(byte[] data, int type) {
        if (currentStreamId == -1) {
            return;
        }
        if (!publishPermitted) {
            return;
        }
        Video video = new Video();
        video.setData(data);
        video.getChunkHeader().setMessageStreamId(currentStreamId);
        Frame<Chunk> frame;
        if(type == RtmpPacker.FIRST_VIDEO) {
            frame = new Frame(video, type, Frame.FRAME_TYPE_CONFIGURATION);
        } else if(type == RtmpPacker.KEY_FRAME){
            frame = new Frame(video, type, Frame.FRAME_TYPE_KEY_FRAME);
        } else {
            frame = new Frame(video, type, Frame.FRAME_TYPE_INTER_FRAME);
        }
        mSendQueue.putFrame(frame);
    }

    public void closeStream() throws IllegalStateException {
        if (currentStreamId == -1) {
            return;
        }
        if (!publishPermitted) {
            return;
        }
        Log.d(TAG, "closeStream(): setting current stream ID to -1");
        Command closeStream = new Command("closeStream", 0);
        closeStream.getChunkHeader().setChunkStreamId(SessionInfo.RTMP_STREAM_CHANNEL);
        closeStream.getChunkHeader().setMessageStreamId(currentStreamId);
        closeStream.addData(new AmfNull());
        Frame<Chunk> frame = new Frame(closeStream, RtmpPacker.CONFIGRATION, Frame.FRAME_TYPE_CONFIGURATION);
        mSendQueue.putFrame(frame);
    }

    public void stop() {
        closeStream();
        if(readThread != null) {
            readThread.setOnReadListener(null);
            readThread.shutdown();
        }
        if(writeThread != null) {
            writeThread.setWriteListener(null);
            writeThread.shutdown();
        }
        clearSocket();
        currentStreamId = -1;
        transactionIdCounter = 0;
        state = State.INIT;
    }

    public State getState() {
        return state;
    }


    @Override
    public void onStreamEnd() {
        stop();
        if(listener != null) {
            listener.onStreamEnd();
        }
    }

    @Override
    public void onDisconnect() {
        stop();
        if(listener != null) {
            listener.onSocketDisconnect();
        }
    }

}
