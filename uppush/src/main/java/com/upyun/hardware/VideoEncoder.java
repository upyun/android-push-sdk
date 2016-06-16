package com.upyun.hardware;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class VideoEncoder {

    private final static String MINE_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String TAG = "VideoEncoder";
    private MediaCodec mediaCodec;
    private int mWidth;
    private int mHeight;
    private byte[] h264;
    private String codecName;
    private byte[] sps_pps;
    public static long startTime;

    public VideoEncoder() {
        initialize();
    }

    protected void initialize() {
        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
            MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
            for (String type : mediaCodecInfo.getSupportedTypes()) {
                if (TextUtils.equals(type, MINE_TYPE)
                        && mediaCodecInfo.isEncoder()) {
                    MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo
                            .getCapabilitiesForType(MINE_TYPE);
                    for (int format : codecCapabilities.colorFormats) {
                        if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                            codecName = mediaCodecInfo.getName();
                            return;
                        }
                    }
                }
            }
        }
    }

    public boolean isSupport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setVideoOptions(int width, int height, int bit, int fps) {
        synchronized (VideoEncoder.class) {
            mWidth = width;
            mHeight = height;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    mediaCodec = MediaCodec.createByCodecName(codecName);
                    MediaFormat mediaFormat = MediaFormat.createVideoFormat(
                            MINE_TYPE, width, height);

                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bit);
                    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 关键帧间隔时间
                    // 单位s
                    mediaFormat
                            .setInteger(
                                    MediaFormat.KEY_COLOR_FORMAT,
                                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);

                    mediaCodec.configure(mediaFormat, null, null,
                            MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mediaCodec.start();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void fireVideo(byte[] data) {
        synchronized (VideoEncoder.class) {

            if (!PushClient.isPush) {
                return;
            }
//        Log.e(TAG, "video data:" + data.length);
            byte[] rawData = nv212nv12(data);
            // 获得编码器输入输出数据缓存区 API:21之后可以使用
            // mediaCodec.getInputBuffer(mediaCodec.dequeueInputBuffer(-1));直接获得缓存数据
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
            // 获得有效输入缓存区数组下标 -1表示一直等待
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
//        Log.d("DEMO", "输入:" + inputBufferIndex);
            if (inputBufferIndex >= 0) {
                // 将原始数据填充 inputbuffers
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(rawData);
                //将此数据加入编码队列 参数3：需要一个增长的时间戳，不然无法持续编码
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, rawData.length,
                        System.nanoTime() / 1000, 0);
            }
            //获得编码后的数据
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            //有效数据下标
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
//        Log.d("DEMO", "输出:" + outputBufferIndex);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//            Log.e(TAG, "编码后大小:" + bufferInfo.size);
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
                if ((outData[4] & 0x1f) == 7) { // sps pps MediaCodec会在编码第一帧之前输出sps+pps sps pps加在一起
                    sps_pps = new byte[outData.length];
                    System.arraycopy(outData, 0, sps_pps, 0, outData.length);

//                Log.d(TAG, "sps pps:" + Arrays.toString(sps_pps));
//                    initSPS(sps_pps, sps_pps.length);
                } else {
                    h264 = new byte[outData.length];
                    System.arraycopy(outData, 0, h264, 0, outData.length);

//                Log.d(TAG, "h264 date:" + Arrays.toString(h264));

                    if ((h264[4] & 0x1f) == 5) {
                        sendVideo(sps_pps, sps_pps.length);
//                    Log.d(TAG, "key fram");
                    }
                    sendVideo(h264, h264.length);
                }
                // 释放编码后的数据
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                // 重新获得编码bytebuffer下标
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
//            Log.d("DEMO", "完成 输出:" + outputBufferIndex);
            }
        }
    }

    private native void send(byte[] data, int length, long time);

    private void sendVideo(byte[] data, int length) {
        synchronized (VideoEncoder.class) {
            long time = System.currentTimeMillis() - startTime;
            send(data, length, time);
        }
    }

    public void init(String url, int width, int Height) {
        synchronized (VideoEncoder.class) {
            startTime = System.currentTimeMillis();
            connect(url, width, Height);
        }
    }

    private native void connect(String url, int width, int Height);

//    private native void initSPS(byte[] data, int length);

    private native void close();

    private byte[] nv212nv12(byte[] data) {
        int len = mWidth * mHeight;
        byte[] buffer = new byte[len * 3 / 2];
        byte[] y = new byte[len];
        byte[] uv = new byte[len / 2];
        System.arraycopy(data, 0, y, 0, len);
        for (int i = 0; i < len / 4; i++) {
            uv[i * 2] = data[len + i * 2 + 1];
            uv[i * 2 + 1] = data[len + i * 2];
        }
        System.arraycopy(y, 0, buffer, 0, y.length);
        System.arraycopy(uv, 0, buffer, y.length, uv.length);
        return buffer;
    }

    public void stop() {

        synchronized (VideoEncoder.class) {

            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
            close();
        }
    }
}