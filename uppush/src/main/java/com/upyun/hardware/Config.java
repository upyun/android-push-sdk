package com.upyun.hardware;

import android.hardware.Camera;

public class Config {

    private static Config config = new Config();

    private Config() {
        this.resolution = Resolution.NORMAL;
        this.url = "rtmp://testlivesdk.v0.upaiyun.com/live/upyunab";
        this.bitRate = 600000;
        this.fps = 30;
        this.cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
        this.orientation = Orientation.VERTICAL;
    }

    private Config setConfig(Builder builder) {
        this.resolution = builder.resolution;
        this.bitRate = builder.bitRate;
        this.url = builder.url;
        this.cameraType = builder.cameraType;
        this.fps = builder.fps;
        this.orientation = builder.orientation;
        return this;
    }

    public static Config getInstance() {
        return config;
    }

    //分辨率 HIGH(1280*720) NORMAL(640*480) LOW(320*240)
    public Resolution resolution;
    //推流地址
    public String url;
    //比特率
    public int bitRate;
    //每秒帧数
    public int fps;
    //摄像头类型 前置，后置
    public int cameraType;
    //显示方向 水平，竖直
    public Orientation orientation;

    public enum Resolution {
        HIGH, NORMAL, LOW
    }

    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    public static final class Builder {
        String url;
        int bitRate;
        int fps;
        int cameraType;
        Resolution resolution;
        Orientation orientation;

        public Builder() {
            url = "rtmp://testlivesdk.v0.upaiyun.com/live/upyunb";
            bitRate = 600000;
            fps = 30;
            cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
            resolution = Resolution.NORMAL;
            orientation = Orientation.VERTICAL;
        }

        public Builder(Config config) {
            this.url = config.url;
            this.bitRate = config.bitRate;
            this.cameraType = config.cameraType;
            this.resolution = config.resolution;
            this.orientation = config.orientation;
        }

        public Builder URL(String url) {
            this.url = url;
            return this;
        }

        public Builder bitRate(int bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        public Builder fps(int fps) {
            this.fps = fps;
            return this;
        }

        public Builder cameraType(int cameraType) {
            this.cameraType = cameraType;
            return this;
        }

        public Builder resolutaion(Resolution resolution) {
            this.resolution = resolution;
            return this;
        }

        public Builder orientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Config build() {
            return config.setConfig(this);
        }
    }
}
