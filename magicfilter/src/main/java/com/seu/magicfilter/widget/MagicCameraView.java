package com.seu.magicfilter.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.seu.magicfilter.camera.CameraEngine;
import com.seu.magicfilter.camera.utils.CameraInfo;
import com.seu.magicfilter.encoder.video.TextureMovieEncoder;
import com.seu.magicfilter.filter.advanced.MagicBeautyFilter;
import com.seu.magicfilter.filter.advanced.MagicWaterFilter;
import com.seu.magicfilter.filter.base.MagicCameraInputFilter;
import com.seu.magicfilter.filter.helper.MagicFilterType;
import com.seu.magicfilter.utils.MagicParams;
import com.seu.magicfilter.utils.OpenGlUtils;
import com.seu.magicfilter.widget.base.MagicBaseView;
import com.upyun.hardware.Watermark;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by why8222 on 2016/2/25.
 */
public class MagicCameraView extends MagicBaseView {

    private static final String TAG = "MagicCameraView";
    private MagicCameraInputFilter cameraInputFilter;
    private MagicBeautyFilter beautyFilter;

    private SurfaceTexture surfaceTexture;

    public MagicCameraView(Context context) {
        this(context, null);
    }

    private boolean recordingEnabled;
    private int recordingStatus;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static TextureMovieEncoder videoEncoder = new TextureMovieEncoder();

    private String outputPath;
    private Object mSurfaceAvailable = new Object();
    private boolean surfaceAvailable = false;

    public MagicCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.getHolder().addCallback(this);
        outputPath = MagicParams.videoPath + MagicParams.videoName;
        recordingStatus = -1;
        recordingEnabled = false;
        scaleType = ScaleType.CENTER_CROP;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        recordingEnabled = videoEncoder.isRecording();
        if (recordingEnabled)
            recordingStatus = RECORDING_RESUMED;
        else
            recordingStatus = RECORDING_OFF;
        if (cameraInputFilter == null)
            cameraInputFilter = new MagicCameraInputFilter();
        cameraInputFilter.init();
        if (textureId == OpenGlUtils.NO_TEXTURE) {
            textureId = OpenGlUtils.getExternalOESTextureID();
            if (textureId != OpenGlUtils.NO_TEXTURE) {
                surfaceTexture = new SurfaceTexture(textureId);
                surfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
            }
        }

        synchronized (mSurfaceAvailable) {
            surfaceAvailable = true;
            mSurfaceAvailable.notify();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);
        openCamera();

        synchronized (mSurfaceAvailable) {
            surfaceAvailable = true;
            mSurfaceAvailable.notify();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        if (surfaceTexture == null)
            return;
        surfaceTexture.updateTexImage();
//        Log.e("onDrawFrame", "recordingEnabled " + recordingEnabled);
        if (recordingEnabled) {
            switch (recordingStatus) {
                case RECORDING_OFF:
                    CameraInfo info = CameraEngine.getCameraInfo();
                    Log.d("CameraInfo:", info.previewWidth + "::" + info.previewHeight);
                    videoEncoder.setPreviewSize(info.previewHeight, info.previewWidth);
                    videoEncoder.setTextureBuffer(gLTextureBuffer);
                    videoEncoder.setCubeBuffer(gLCubeBuffer);
                    videoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            outputPath, info.previewHeight, info.previewWidth,
                            700000, EGL14.eglGetCurrentContext(),
                            info));
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    videoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    recordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    if (filterChanged) {
                        videoEncoder.changeFilter();
                        filterChanged = false;
                    }
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        } else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    videoEncoder.stopRecording();
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        }
        float[] mtx = new float[16];
        surfaceTexture.getTransformMatrix(mtx);
        cameraInputFilter.setTextureTransformMatrix(mtx);
        int id = textureId;
        if (filter == null) {
            cameraInputFilter.onDrawFrame(textureId, gLCubeBuffer, gLTextureBuffer);
        } else {
            id = cameraInputFilter.onDrawToTexture(textureId);
            filter.onDrawFrame(id, gLCubeBuffer, gLTextureBuffer);
        }
        videoEncoder.setTextureId(id);
        videoEncoder.frameAvailable(surfaceTexture);
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            requestRender();
        }
    };

    @Override
    public void setFilter(MagicFilterType type) {
        super.setFilter(type);
        videoEncoder.setFilter(type);
    }

    private void openCamera() {
        if (CameraEngine.getCamera() == null)
            CameraEngine.openCamera();
        CameraInfo info = CameraEngine.getCameraInfo();
        if (info.orientation == 90 || info.orientation == 270) {
            imageWidth = info.previewHeight;
            imageHeight = info.previewWidth;
        } else {
            imageWidth = info.previewWidth;
            imageHeight = info.previewHeight;
        }
        cameraInputFilter.onInputSizeChanged(imageWidth, imageHeight);
        adjustSize(info.orientation, info.isFront, true);
        if (surfaceTexture != null)
            CameraEngine.startPreview(surfaceTexture);
    }


    public void switchCamera() {
        CameraEngine.switchCamera();
        CameraInfo info = CameraEngine.getCameraInfo();
        if (info.orientation == 90 || info.orientation == 270) {
            imageWidth = info.previewHeight;
            imageHeight = info.previewWidth;
        } else {
            imageWidth = info.previewWidth;
            imageHeight = info.previewHeight;
        }
        cameraInputFilter.onInputSizeChanged(imageWidth, imageHeight);
        adjustSize(info.orientation, info.isFront, true);
        if (surfaceTexture != null)
            CameraEngine.startPreview(surfaceTexture);
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        CameraEngine.releaseCamera();
        synchronized (mSurfaceAvailable) {
            surfaceAvailable = false;
        }
    }

    public void changeRecordingState(boolean isRecording) {

        synchronized (mSurfaceAvailable) {
            while (!surfaceAvailable) {
                try {
                    mSurfaceAvailable.wait();
                    Log.e(TAG, "changeRecordingState: " + surfaceAvailable);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            recordingEnabled = isRecording;
        }
    }

    protected void onFilterChanged() {
        super.onFilterChanged();
        cameraInputFilter.onDisplaySizeChanged(surfaceWidth, surfaceHeight);
        if (filter != null)
            cameraInputFilter.initCameraFrameBuffer(imageWidth, imageHeight);
        else
            cameraInputFilter.destroyFramebuffers();
    }

    public void onBeautyLevelChanged() {
        cameraInputFilter.onBeautyLevelChanged();
    }

    public void setWatermark(final Watermark watermark) {
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                MagicWaterFilter.mWatermark = watermark;
                setFilter(MagicFilterType.WATERMARK);
            }
        }, 1000);
    }
}
