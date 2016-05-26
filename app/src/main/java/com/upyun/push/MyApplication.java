package com.upyun.push;

import android.app.Application;

import com.upyun.hardware.Config;

public class MyApplication extends Application{

    public Config config;
    private static MyApplication instance;

    public static MyApplication getInstance(){
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
