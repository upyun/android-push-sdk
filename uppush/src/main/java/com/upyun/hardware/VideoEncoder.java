package com.upyun.hardware;

import android.annotation.TargetApi;
import android.hardware.Camera;
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

    private int mPushWidth;
    private int mPushHeight;
    private int mColorFormats;

    private byte[] mRotatedFrameBuffer;
    private byte[] mFlippedFrameBuffer;
    private byte[] mCroppedFrameBuffer;

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
                            mColorFormats = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
                            return;
                        } else if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                            codecName = mediaCodecInfo.getName();
                            mColorFormats = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
                            return;
                        }
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setVideoOptions(int width, int height, int bit, int fps) {
        synchronized (VideoEncoder.class) {
            mWidth = width;
            mHeight = height;

            mPushWidth = mHeight;
            mPushHeight = mWidth;

            mRotatedFrameBuffer = new byte[mWidth * mHeight * 3 / 2];
            mFlippedFrameBuffer = new byte[mWidth * mHeight * 3 / 2];
            mCroppedFrameBuffer = new byte[mWidth * mHeight * 3 / 2];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    mediaCodec = MediaCodec.createByCodecName(codecName);
                    MediaFormat mediaFormat = MediaFormat.createVideoFormat(
                            MINE_TYPE, mPushWidth, mPushHeight);

                    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bit);
                    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
                    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 关键帧间隔时间
                    // 单位s
                    mediaFormat
                            .setInteger(
                                    MediaFormat.KEY_COLOR_FORMAT,
                                    mColorFormats);

                    mediaCodec.configure(mediaFormat, null, null,
                            MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mediaCodec.start();
                } catch (Exception e) {
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

            preprocessYUVData(data);

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
                inputBuffer.put(mRotatedFrameBuffer);
                //将此数据加入编码队列 参数3：需要一个增长的时间戳，不然无法持续编码
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, mRotatedFrameBuffer.length,
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

    private void preprocessYUVData(byte[] data) {
        if (PushClient.CAMERA_TYPE == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            switch (mColorFormats) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    cropYUV420SemiPlannerFrame(data, mWidth, mHeight, mCroppedFrameBuffer, mPushHeight, mPushWidth);
                    flipYUV420SemiPlannerFrame(mCroppedFrameBuffer, mFlippedFrameBuffer, mPushHeight, mPushWidth);
                    rotateYUV420SemiPlannerFrame(mFlippedFrameBuffer, mRotatedFrameBuffer, mPushHeight, mPushWidth);
                    mRotatedFrameBuffer = yuv420sp_to_yuv420p(mRotatedFrameBuffer);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    cropYUV420SemiPlannerFrame(data, mWidth, mHeight, mCroppedFrameBuffer, mPushHeight, mPushWidth);
                    flipYUV420SemiPlannerFrame(mCroppedFrameBuffer, mFlippedFrameBuffer, mPushHeight, mPushWidth);
                    rotateYUV420SemiPlannerFrame(mFlippedFrameBuffer, mRotatedFrameBuffer, mPushHeight, mPushWidth);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }

        } else {
            switch (mColorFormats) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    cropYUV420SemiPlannerFrame(data, mWidth, mHeight, mCroppedFrameBuffer, mPushHeight, mPushWidth);
                    rotateYUV420SemiPlannerFrame(data, mRotatedFrameBuffer, mPushHeight, mPushWidth);
                    mRotatedFrameBuffer = yuv420sp_to_yuv420p(mRotatedFrameBuffer);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    cropYUV420SemiPlannerFrame(data, mWidth, mHeight, mCroppedFrameBuffer, mPushHeight, mPushWidth);
                    rotateYUV420SemiPlannerFrame(mCroppedFrameBuffer, mRotatedFrameBuffer, mPushHeight, mPushWidth);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
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

    public void init(String url) {
        synchronized (VideoEncoder.class) {
            startTime = System.currentTimeMillis();
            connect(url, mPushWidth, mPushHeight);
        }
    }

    private native void connect(String url, int width, int Height);

//    private native void initSPS(byte[] data, int length);

    private native void close();

    // Y, U (Cb) and V (Cr)
    // yuv420                     yuv yuv yuv yuv
    // yuv420p (planar)   yyyy*2 uu vv
    // yuv420sp(semi-planner)   yyyy*2 uv uv
    // I420 -> YUV420P   yyyy*2 uu vv
    // YV12 -> YUV420P   yyyy*2 vv uu
    // NV21 -> YUV420SP  yyyy*2 vu vu
    // NV12 -> YUV420SP  yyyy*2 uv uv
    // NV16 -> YUV422SP  yyyy uv uv
    // YUY2 -> YUV422SP  yuyv yuyv
    private byte[] cropYUV420SemiPlannerFrame(byte[] input, int iw, int ih, byte[] output, int ow, int oh) {
        if (iw < ow || ih < oh) {
            throw new AssertionError("Crop revolution size must be less than original one");
        }
        if (ow % 32 != 0 || oh % 32 != 0) {
            // Note: the stride of resolution must be set as 16x for hard encoding with some chip like MTK
            // Since Y component is quadruple size as U and V component, the stride must be set as 32x
            throw new AssertionError("MTK encoding revolution stride must be 32x");
        }

        int iFrameSize = iw * ih;
        int oFrameSize = ow * oh;

        int i = 0;
        for (int row = (ih - oh) / 2; row < oh + (ih - oh) / 2; row++) {
            for (int col = (iw - ow) / 2; col < ow + (iw - ow) / 2; col++) {
                output[i++] = input[iw * row + col];  // Y
            }
        }

        i = 0;
        for (int row = (ih - oh) / 4; row < oh / 2 + (ih - oh) / 4; row++) {
            for (int col = (iw - ow) / 4; col < ow / 2 + (iw - ow) / 4; col++) {
                output[oFrameSize + 2 * i] = input[iFrameSize + iw * row + 2 * col];  // U
                output[oFrameSize + 2 * i + 1] = input[iFrameSize + iw * row + 2 * col + 1];  // V
                i++;
            }
        }

        return output;
    }


    // 1. rotate 90 degree clockwise
    // 2. convert NV21 to NV12
    private byte[] rotateYUV420SemiPlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;

        int i = 0;
        for (int col = 0; col < width; col++) {
            for (int row = height - 1; row >= 0; row--) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int col = 0; col < width / 2; col++) {
            for (int row = height / 2 - 1; row >= 0; row--) {
                output[frameSize + i * 2 + 1] = input[frameSize + width * row + col * 2]; // Cb (U)
                output[frameSize + i * 2] = input[frameSize + width * row + col * 2 + 1]; // Cr (V)
                i++;
            }
        }

        return output;
    }

    private byte[] flipYUV420SemiPlannerFrame(byte[] input, byte[] output, int width, int height) {
        int frameSize = width * height;

        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = width - 1; col >= 0; col--) {
                output[i++] = input[width * row + col]; // Y
            }
        }

        i = 0;
        for (int row = 0; row < height / 2; row++) {
            for (int col = width / 2 - 1; col >= 0; col--) {
                output[frameSize + i * 2] = input[frameSize + width * row + col * 2]; // Cb (U)
                output[frameSize + i * 2 + 1] = input[frameSize + width * row + col * 2 + 1]; // Cr (V)
                i++;
            }
        }

        return output;
    }

    // nv12 >> I420
    private byte[] yuv420sp_to_yuv420p(byte[] data) {
        int len = mWidth * mHeight;
        byte[] buffer = new byte[len * 3 / 2];
        byte[] y = new byte[len];
        byte[] u = new byte[len / 4];
        byte[] v = new byte[len / 4];
        System.arraycopy(data, 0, y, 0, len);
        for (int i = 0; i < len / 4; i++) {
            u[i] = data[len + i * 2];
            v[i] = data[len + i * 2 + 1];
        }
        System.arraycopy(y, 0, buffer, 0, len);
        System.arraycopy(u, 0, buffer, len, len / 4);
        System.arraycopy(v, 0, buffer, len * 5 / 4, len / 4);
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