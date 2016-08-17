package com.upyun.push;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.upyun.hardware.Config;
import com.upyun.hardware.PushClient;

import java.io.IOException;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private SurfaceView surface;
    private PushClient mClient;
    private Button mBtToggle;
    private Button mBtSetting;
    private Button mBtconvert;
    private Config config;
    private String mNotifyMsg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surface = (SurfaceView) findViewById(R.id.sv_camera);
        mBtToggle = (Button) findViewById(R.id.bt_toggle);
        mBtSetting = (Button) findViewById(R.id.bt_setting);
        mBtconvert = (Button) findViewById(R.id.bt_convert);
        mBtToggle.setOnClickListener(this);
        mBtSetting.setOnClickListener(this);
        mBtconvert.setOnClickListener(this);

        mClient = new PushClient(surface);


        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                mNotifyMsg = ex.toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, mNotifyMsg);
                        mClient.stopPush();
                        mBtToggle.setText("start");
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        config = MyApplication.getInstance().config;
        if (config != null) {
            mClient.setConfig(config);
        }

        changeSurfaceSize(surface, mClient.getConfig());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_toggle:
                if (mClient.isStart()) {
                    mClient.stopPush();
                    Log.e(TAG, "stop");
                    mBtToggle.setText("start");
                } else {
                    try {
                        mClient.startPush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, e.toString());
                        return;
                    }
                    Log.e(TAG, "start");
                    mBtToggle.setText("stop");
                }
                break;
            case R.id.bt_setting:
                startActivity(new Intent(this, SettingActivity.class));
                break;

            case R.id.bt_convert:
                mClient.covertCamera();
                break;
        }
    }

    private void changeSurfaceSize(SurfaceView surface, Config config) {
        int width = 1280;
        int height = 720;

        switch (config.resolution) {
            case HIGH:
                width = 1280;
                height = 720;
                break;
            case NORMAL:
//                width = 640;
//                height = 480;
//                break;
            case LOW:
                width = 320 * 3;
                height = 240 * 3;
                break;
        }

        ViewGroup.LayoutParams lp = surface.getLayoutParams();

        lp.width = height;
        lp.height = width;
        surface.setLayoutParams(lp);
    }
}

