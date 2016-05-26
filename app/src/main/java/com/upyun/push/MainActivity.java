package com.upyun.push;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.upyun.hardware.Config;
import com.upyun.hardware.PushClient;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private SurfaceView surface;
    private PushClient mClient;
    private Button mBtToggle;
    private Button mBtSetting;
    private Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surface = (SurfaceView) findViewById(R.id.sv_camera);
        mBtToggle = (Button) findViewById(R.id.bt_toggle);
        mBtSetting = (Button) findViewById(R.id.bt_setting);
        mBtToggle.setOnClickListener(this);
        mBtSetting.setOnClickListener(this);

        mClient = new PushClient(surface);
    }

    @Override
    protected void onStart() {
        super.onStart();
        config = MyApplication.getInstance().config;
        if (config != null) {
            mClient.setConfig(config);
        }
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
                    mClient.startPush();
                    Log.e(TAG, "start");
                    mBtToggle.setText("stop");
                }
                break;
            case R.id.bt_setting:
                startActivity(new Intent(this, SettingActivity.class));
                break;
        }

    }
}

