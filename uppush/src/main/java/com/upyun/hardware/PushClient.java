package com.upyun.hardware;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import net.ossrs.yasea.SrsFlvMuxer;
import net.ossrs.yasea.rtmp.RtmpPublisher;

import java.io.IOException;
import java.util.List;


public class PushClient implements Camera.PreviewCallback, SurfaceHolder.Callback {

    private static final String TAG = "PushClient";
    private SurfaceView mSurface;

    private Config config;
    private Camera mCamera;
    private byte[] mYuvFrameBuffer;
    private int width;
    private int height;
    private VideoEncoder videoEncoder;
    private AudioEncoder audioEncoder;
    private AudioRecord audioRecord;
    private boolean aloop = false;
    private Thread aworker = null;
    protected static boolean isPush = false;

    protected final static int MODE_NORMAL = 1;
    protected final static int MODE_VIDEO_ONLY = 2;
    protected final static int MODE_AUDIO_ONLY = 3;

    //    protected static int MODE = MODE_NORMAL;
    protected static int MODE = MODE_NORMAL;
    //    protected static int MODE = MODE_VIDEO_ONLY;
    protected static int CAMERA_TYPE;

    private SrsFlvMuxer mSrsFlvMuxer = new SrsFlvMuxer(new RtmpPublisher.EventHandler() {
        @Override
        public void onRtmpConnecting(String msg) {
            Log.e(TAG, msg);
        }

        @Override
        public void onRtmpConnected(String msg) {
            Log.e(TAG, msg);
        }

        @Override
        public void onRtmpVideoStreaming(String msg) {
        }

        @Override
        public void onRtmpAudioStreaming(String msg) {
        }

        @Override
        public void onRtmpStopped(String msg) {
            Log.e(TAG, msg);
        }

        @Override
        public void onRtmpDisconnected(String msg) {
            Log.e(TAG, msg);
        }

        @Override
        public void onRtmpOutputFps(final double fps) {
            Log.i(TAG, String.format("Output Fps: %f", fps));
        }
    });

    public PushClient(SurfaceView surface) {
        this(surface, new Config.Builder().build());
    }

    public Config getConfig() {
        return config;
    }

    public PushClient(SurfaceView surface, Config config) {
        this.mSurface = surface;
        this.config = config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    private void startCamera(final SurfaceHolder holder) {

        CAMERA_TYPE = config.cameraType;

        if (mCamera == null) {
            mCamera = getDefaultCamera(config.cameraType);
        } else {
            return;
        }

        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(PushClient.this);

        switch (config.resolution) {
            case HIGH:
                width = 1280;
                height = 720;
                break;
            case NORMAL:
                width = 640;
                height = 480;
                break;
            case LOW:
                width = 320;
                height = 240;
                break;
        }

        mYuvFrameBuffer = new byte[width * height * 3 / 2];

        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();

            parameters.setPreviewSize(width, height);
            int[] range = findClosestFpsRange(config.fps, parameters.getSupportedPreviewFpsRange());
            parameters.setPreviewFpsRange(range[0], range[1]);
            parameters.setPreviewFormat(ImageFormat.NV21);
            if (parameters.getSupportedFocusModes() != null &&
                parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(PushClient.this);
            mCamera.addCallbackBuffer(mYuvFrameBuffer);
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        }

    }


    private void stopCamera() {
        synchronized (Camera.class) {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }
    }


    private void stopAudio() {
        aloop = false;
        if (aworker != null) {
            Log.i(TAG, "stop audio worker thread");
            aworker.interrupt();
            try {
                aworker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                aworker.interrupt();
            }
            aworker = null;
        }

        if (audioRecord != null) {
            audioRecord.setRecordPositionUpdateListener(null);
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

//        Log.e(TAG, "data:" + data.length);
        if (videoEncoder != null && isPush) {
            videoEncoder.fireVideo(data);
            camera.addCallbackBuffer(mYuvFrameBuffer);
        }
    }

    private int[] findClosestFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    public void startPush() throws IOException {

        if (isPush) {
            return;
        }
        isPush = true;
        if (mCamera == null) {
            startCamera(mSurface.getHolder());
        }
        videoEncoder = new VideoEncoder(mSrsFlvMuxer, config);
        audioEncoder = new AudioEncoder(mSrsFlvMuxer);
        videoEncoder.setVideoOptions(width,
                height, config.bitRate, config.fps);
//        videoEncoder.init(config.url);
        mSrsFlvMuxer.start(config.url);

        aworker = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                startAudio();
            }
        });
        aloop = true;

        aworker.start();
    }

    private void startAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC, 44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        audioRecord.startRecording();
        while (aloop && !Thread.interrupted()) {

            byte[] buffer = new byte[minBufferSize];
            int len = audioRecord.read(buffer, 0, minBufferSize);
            if (0 < len) {
                audioEncoder.fireAudio(buffer, len);
            }
        }

    }

    public void stopPush() {
        isPush = false;
        if (mSrsFlvMuxer != null) {
            mSrsFlvMuxer.stop();
        }
        stopCamera();
        stopAudio();
        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder = null;
        }
        if (videoEncoder != null) {
            videoEncoder.stop();
            videoEncoder = null;
        }
    }

    public static Camera getDefaultCamera(int position) {

        synchronized (Camera.class) {

            // Find the total number of cameras available
            int mNumberOfCameras = Camera.getNumberOfCameras();

            // Find the ID of the back-facing ("default") camera
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < mNumberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == position) {
                    try {
                        return Camera.open(i);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage());

                    }
                }
            }

            return null;
        }

    }

    public boolean isStart() {
        return isPush;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public boolean covertCamera() {
        boolean converted = false;
        if (isPush) {
            if (config.cameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                config.cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
            } else {
                config.cameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
            stopCamera();
            startCamera(mSurface.getHolder());
            converted = true;
        }
        return converted;
    }

    public void focusOnTouch() {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (!parameters.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO) &&
                    parameters.getSupportedFocusModes() != null &&
                    parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(parameters);
            }

            mCamera.cancelAutoFocus();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        Log.e(TAG, "auto focus success");
                    } else {
                        Log.e(TAG, "auto focus failed");
                    }

                    //resume the continuous focus
                    Camera.Parameters parameters = camera.getParameters();
                    camera.cancelAutoFocus();
                    if (parameters.getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE &&
                            parameters.getSupportedFocusModes() != null &&
                            parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        camera.setParameters(parameters);
                    }
                }
            });
        }
    }

    // flash light
    public void toggleFlashlight() {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getFlashMode() != null) {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    if (parameters.getSupportedFlashModes() != null &&
                            parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    }
                } else {
                    if (parameters.getSupportedFlashModes() != null &&
                            parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_OFF)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    }
                }
                mCamera.setParameters(parameters);
            } else {
                Log.e(TAG, "The device does not support control of a flashlight!");
            }
        }
    }
}



