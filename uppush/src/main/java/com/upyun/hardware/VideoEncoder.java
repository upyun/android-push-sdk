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

import net.ossrs.yasea.SrsFlvMuxer;

import java.nio.ByteBuffer;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class VideoEncoder {

    private final static String MINE_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String TAG = "VideoEncoder";
    private MediaCodec mediaCodec;
    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mBitrate;
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

    private int mVideoTrack;
    private SrsFlvMuxer mflvmuxer;
    private boolean isStarted = false;
    private Config config;

    private SoftEncoder softEncoder;

    private int cropX;
    private int cropY;

    public VideoEncoder(SrsFlvMuxer flvMuxer, Config config) {
        this.mflvmuxer = flvMuxer;
        this.config = config;
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
        Log.i(TAG, "userSoft:" + config.useSofeEncode + " bitRate:" + bit
                + " width:" + width + " height:" + height + " fps:" + fps);
        mWidth = width;
        mHeight = height;
        mFps = fps;
        mBitrate = bit;

        mPushWidth = mWidth;
        mPushHeight = mHeight;
//        mPushHeight = 360;

        if (mWidth != mPushWidth || mHeight != mPushHeight) {
            cropX = (mWidth - mPushWidth) / 2;
            cropY = (mHeight - mPushHeight) / 2;
        }

        //for MTK CPU
//        if (config.orientation == Config.Orientation.HORIZONTAL) {
//            mPushWidth = mWidth / 32 * 32;
//            mPushHeight = mHeight / 32 * 32;
//        } else {
//            mPushWidth = mHeight / 32 * 32;
//            mPushHeight = mWidth / 32 * 32;
//        }

//        if (config.orientation == Config.Orientation.HORIZONTAL) {
//            mPushWidth = mWidth / 32 * 32;
//            mPushHeight = mWidth * 9 / 16 / 32 * 32;
//        } else {
//            mPushWidth = mWidth * 9 / 16 / 32 * 32;
//            mPushHeight = mWidth / 32 * 32;
//        }

        softEncoder = new SoftEncoder(this);

        int outWidth;

        int outHeight;

        if (config.orientation == Config.Orientation.HORIZONTAL) {
            outWidth = mPushWidth;
            outHeight = mPushHeight;

        } else {
            outWidth = mPushHeight;
            outHeight = mPushWidth;
        }

        softEncoder.setEncoderResolution(outWidth, outHeight);
        softEncoder.setEncoderFps(fps);
        softEncoder.setEncoderGop(15);
        // Unfortunately for some android phone, the output fps is less than 10 limited by the
        // capacity of poor cheap chips even with x264. So for the sake of quick appearance of
        // the first picture on the player, a spare lower GOP value is suggested. But note that
        // lower GOP will produce more I frames and therefore more streaming data flow.
        softEncoder.setEncoderBitrate(bit);
        softEncoder.setEncoderPreset("veryfast");

        softEncoder.openSoftEncoder();


        mRotatedFrameBuffer = new byte[mPushHeight * mPushWidth * 3 / 2];
        mFlippedFrameBuffer = new byte[mPushHeight * mPushWidth * 3 / 2];
        mCroppedFrameBuffer = new byte[mPushHeight * mPushWidth * 3 / 2];

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                mediaCodec = MediaCodec.createByCodecName(codecName);
                MediaFormat mediaFormat = MediaFormat.createVideoFormat(
                        MINE_TYPE, outWidth, outHeight);

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

                if (PushClient.MODE != PushClient.MODE_AUDIO_ONLY) {
                    mVideoTrack = mflvmuxer.addTrack(mediaFormat);
                    mflvmuxer.setVideoResolution(outWidth, outHeight);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void adjustBitrate(int bitrate) {
        if (mBitrate == bitrate) {
            Log.w(TAG, "The bitrate is not changed.");
            return;
        }

        synchronized (VideoEncoder.class) {
            if (mediaCodec != null) {
                MediaFormat mediaFormat = MediaFormat.createVideoFormat(
                        MINE_TYPE, mPushWidth, mPushHeight);

                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); // 关键帧间隔时间
                // 单位s
                mediaFormat
                        .setInteger(
                                MediaFormat.KEY_COLOR_FORMAT,
                                mColorFormats);

                mediaCodec.stop();
                mediaCodec.configure(mediaFormat, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                mediaCodec.start();
                mBitrate = bitrate;
            }
        }
    }


    public void fireVideo(byte[] data, long stamp) {
        if (data == null || (data.length != mWidth * mHeight * 3 / 2)) {
            Log.w(TAG, "firevideo Illegal data");
            return;
        }
        synchronized (VideoEncoder.class) {

            if (PushClient.MODE == PushClient.MODE_AUDIO_ONLY) {
                return;
            }
            if (mflvmuxer.getVideoFrameCacheNumber().get() > 5) {
                return;
            }
            if (!PushClient.isPush) {
                return;
            }

            if (config.useSofeEncode) {
                if (config.orientation == Config.Orientation.HORIZONTAL) {
                    softEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 0, stamp,
                            cropX, cropY, mPushWidth, mPushHeight);
                } else {
                    if (config.cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        softEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 270, stamp,
                                cropX, cropY, mPushWidth, mPushHeight);
                    } else {
                        softEncoder.NV21SoftEncode(data, mWidth, mHeight, false, 90, stamp,
                                cropX, cropY, mPushWidth, mPushHeight);
                    }
                }
            } else {
                if (config.orientation == Config.Orientation.HORIZONTAL) {
                    preprocessYUVDataH(data);
                } else {
                    preprocessYUVData(data);
                }
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
                            stamp, 0);
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
    }

    private void preprocessYUVData(byte[] data) {
        if (config.cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            switch (mColorFormats) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    mRotatedFrameBuffer = softEncoder.NV21ToI420(data, mWidth, mHeight,
                            false, 270, cropX, cropY, mPushWidth, mPushHeight);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    mRotatedFrameBuffer = softEncoder.NV21ToNV12(data, mWidth, mHeight,
                            false, 270, cropX, cropY, mPushWidth, mPushHeight);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }

        } else {
            switch (mColorFormats) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    mRotatedFrameBuffer = softEncoder.NV21ToI420(data, mWidth, mHeight,
                            false, 90, cropX, cropY, mPushWidth, mPushHeight);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    mRotatedFrameBuffer = softEncoder.NV21ToNV12(data, mWidth, mHeight,
                            false, 90, cropX, cropY, mPushWidth, mPushHeight);
                    break;
                default:
                    throw new IllegalStateException("Unsupported color format!");
            }
        }
    }

    private void preprocessYUVDataH(byte[] data) {
        switch (mColorFormats) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                mRotatedFrameBuffer = softEncoder.NV21ToI420(data, mWidth, mHeight,
                        false, 0, cropX, cropY, mPushWidth, mPushHeight);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                mRotatedFrameBuffer = softEncoder.NV21ToNV12(data, mWidth, mHeight,
                        false, 0, cropX, cropY, mPushWidth, mPushHeight);
                break;
            default:
                throw new IllegalStateException("Unsupported color format!");
        }
    }


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
        if (mWidth == mPushWidth && mHeight == mPushHeight) {
            mCroppedFrameBuffer = input;
            return input;
        }

        if (iw < ow || ih < oh) {
            throw new AssertionError("Crop revolution size must be less than original one");
        }
//        if (ow % 32 != 0 || oh % 32 != 0) {
//            // Note: the stride of resolution must be set as 16x for hard encoding with some chip like MTK
//            // Since Y component is quadruple size as U and V component, the stride must be set as 32x
//            throw new AssertionError("MTK encoding revolution stride must be 32x");
//        }

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
        int len = mPushHeight * mPushWidth;
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

            if (softEncoder != null) {
                softEncoder.closeSoftEncoder();
                softEncoder = null;
            }
        }
    }

    public void onEncodedAnnexbFrame(ByteBuffer bb, MediaCodec.BufferInfo vebi) {
        if (mflvmuxer != null && mVideoTrack != 0) {
            mflvmuxer.writeSampleData(mVideoTrack, bb, vebi);
        }
    }
}