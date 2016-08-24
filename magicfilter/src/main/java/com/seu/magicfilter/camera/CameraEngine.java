package com.seu.magicfilter.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;

import com.seu.magicfilter.utils.MagicParams;

import java.io.IOException;

public class CameraEngine {
    private static Camera camera = null;
    private static int cameraID = 1;
    private static SurfaceTexture surfaceTexture;

    public static Camera getCamera() {
        return camera;
    }

    public static boolean openCamera() {
        if (camera == null) {
            try {
                camera = Camera.open(cameraID);
                setDefaultParameters();
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    public static boolean openCamera(int id) {
        if (camera == null) {
            try {
                camera = Camera.open(id);
                cameraID = id;
                setDefaultParameters();
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    public static void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void resumeCamera() {
        openCamera();
    }

    public void setParameters(Parameters parameters) {
        camera.setParameters(parameters);
    }

    public Parameters getParameters() {
        if (camera != null)
            camera.getParameters();
        return null;
    }

    public static void switchCamera() {
        releaseCamera();
        cameraID = cameraID == 0 ? 1 : 0;
        openCamera(cameraID);
        startPreview(surfaceTexture);
    }

    public static void switchFlashlight() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            boolean flashOn = parameters.getFlashMode().equals(Parameters.FLASH_MODE_OFF) ? false : true;
            if (flashOn) {
                if (parameters.getSupportedFlashModes().contains(
                        Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                }
            } else {
                if (parameters.getSupportedFlashModes().contains(
                        Parameters.FLASH_MODE_TORCH)) {
                    parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                }
            }
            camera.setParameters(parameters);
        }
    }

    private static void setDefaultParameters() {
        Parameters parameters = camera.getParameters();
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
//        Size previewSize = CameraUtils.getLargePreviewSize(camera);
//        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setPreviewSize(MagicParams.WIDTH, MagicParams.HEIGHT);
//        parameters.setPreviewSize(480, 640);
//        Size pictureSize = CameraUtils.getLargePictureSize(camera);
//        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        parameters.setPictureSize(MagicParams.WIDTH, MagicParams.HEIGHT);
//        parameters.setPictureSize(480, 640);
        parameters.setRotation(90);
        camera.setParameters(parameters);
    }

    private static Size getPreviewSize() {
        return camera.getParameters().getPreviewSize();
    }

    private static Size getPictureSize() {
        return camera.getParameters().getPictureSize();
    }

    public static void startPreview(SurfaceTexture surfaceTexture) {
        if (camera != null)
            try {
                camera.setPreviewTexture(surfaceTexture);
                CameraEngine.surfaceTexture = surfaceTexture;
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static void startPreview() {
        if (camera != null)
            camera.startPreview();
    }

    public static void stopPreview() {
        camera.stopPreview();
    }

    public static void setRotation(int rotation) {
        Camera.Parameters params = camera.getParameters();
        params.setRotation(rotation);
        camera.setParameters(params);
    }

    public static void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
                                   Camera.PictureCallback jpegCallback) {
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    public static com.seu.magicfilter.camera.utils.CameraInfo getCameraInfo() {
        com.seu.magicfilter.camera.utils.CameraInfo info = new com.seu.magicfilter.camera.utils.CameraInfo();
        Size size = getPreviewSize();
        Log.e("getPreviewSize:", size.width + "::" + size.height);

        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraID, cameraInfo);

        info.orientation = cameraInfo.orientation;
        info.isFront = cameraID == 1 ? true : false;

        /*if (info.orientation == 90 || info.orientation == 270) {
            info.previewWidth = size.height;
            info.previewHeight = size.width;
        } else {
            info.previewWidth = size.width;
            info.previewHeight = size.height;
        }*/
        info.previewWidth = size.width;
        info.previewHeight = size.height;

        size = getPictureSize();

        Log.e("getPictureSize:", size.width + "::" + size.height);
        info.pictureWidth = size.width;
        info.pictureHeight = size.height;
        return info;
    }
}