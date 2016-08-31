package com.upyun.push;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
    private static final int REQUEST_CODE_PERMISSION_CAMERA = 100;
    private static final int REQUEST_CODE_PERMISSION_RECORD_AUDIO = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        // check permission for 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }
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
                mClient.stopPush();
                mBtToggle.setText("start");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            mClient.stopPush();
        }
    }

    // check permission
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "granted camera permission");
            } else {
                Log.e(TAG, "no camera permission");
            }
        } else if (requestCode == REQUEST_CODE_PERMISSION_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "granted record audio permission");
            } else {
                Log.d(TAG, "no record audio permission");
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}

