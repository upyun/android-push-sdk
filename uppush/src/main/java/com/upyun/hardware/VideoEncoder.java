package com.upyun.hardware;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;

import net.ossrs.yasea.SrsFlvMuxer;

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
    private int mVideoTrack;
    private SrsFlvMuxer mflvmuxer;

    public VideoEncoder(SrsFlvMuxer flvMuxer) {
        mflvmuxer = flvMuxer;
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
        mWidth = width;
        mHeight = height;

        mPushWidth = mHeight;
        mPushHeight = mWidth;

        mRotatedFrameBuffer = new byte[mWidth * mHeight * 3 / 2];
        mFlippedFrameBuffer = new byte[mWidth * mHeight * 3 / 2];

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

                mVideoTrack = mflvmuxer.addTrack(mediaFormat);
                mflvmuxer.setVideoResolution(mPushWidth, mPushHeight);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fireVideo(byte[] data) {
        synchronized (VideoEncoder.class) {

            if (mflvmuxer.getVideoFrameCacheNumber().get() > 5) {
                return;
            }

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
            while (outputBufferIndex >= 0 && PushClient.isPush) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//            Log.e(TAG, "编码后大小:" + bufferInfo.size);

                mflvmuxer.writeSampleData(mVideoTrack, outputBuffer, bufferInfo);

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
                    flipYUV420SemiPlannerFrame(data, mFlippedFrameBuffer, mPushHeight, mPushWidth);
                    rotateYUV420SemiPlannerFrame(mFlippedFrameBuffer, mRotatedFrameBuffer, mPushHeight, mPushWidth);
                    mRotatedFrameBuffer = yuv420sp_to_yuv420p(mRotatedFrameBuffer);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    flipYUV420SemiPlannerFrame(data, mFlippedFrameBuffer, mPushHeight, mPushWidth);
                    rotateYUV420SemiPlannerFrame(mFlippedFrameBuffer, mRotatedFrameBuffer, mPushHeight, mPushWidth);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }

        } else {
            switch (mColorFormats) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    rotateYUV420SemiPlannerFrame(data, mRotatedFrameBuffer, mPushHeight, mPushWidth);
                    mRotatedFrameBuffer = yuv420sp_to_yuv420p(mRotatedFrameBuffer);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    rotateYUV420SemiPlannerFrame(data, mRotatedFrameBuffer, mPushHeight, mPushWidth);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }
        }
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
        }
    }
}