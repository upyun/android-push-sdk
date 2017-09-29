package com.upyun.push;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.upyun.hardware.Config;

public class SettingActivity extends Activity implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {

    private static final String TAG = "SettingActivity";
    private EditText mEtUrl;
    private EditText mEtFps;
    private EditText mEtBitrate;
    private RadioGroup mRgResolution;
    private RadioGroup mRgCamera;
    private RadioGroup mRgOrientation;
    private RadioGroup mRgEncode;
    private Button mBtSave;
    private Config config;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
    }

    private void initView() {
        mEtUrl = (EditText) findViewById(R.id.et_url);
        mEtFps = (EditText) findViewById(R.id.et_fps);
        mEtBitrate = (EditText) findViewById(R.id.et_bitrate);
        mRgResolution = (RadioGroup) findViewById(R.id.rg_resolution);
        mRgCamera = (RadioGroup) findViewById(R.id.rg_camera);
        mRgOrientation = (RadioGroup) findViewById(R.id.rg_orientation);
        mRgEncode = (RadioGroup) findViewById(R.id.rg_encode);
        mBtSave = (Button) findViewById(R.id.bt_save);

        mRgResolution.setOnCheckedChangeListener(this);
        mRgCamera.setOnCheckedChangeListener(this);
        mRgOrientation.setOnCheckedChangeListener(this);
        mRgEncode.setOnCheckedChangeListener(this);
        mBtSave.setOnClickListener(this);
        if (MyApplication.getInstance().config == null) {
            this.config = Config.getInstance();
        } else {
            this.config = MyApplication.getInstance().config;
        }
        mEtUrl.setText(config.url);
        mEtFps.setText(String.valueOf(config.fps));
        mEtBitrate.setText(String.valueOf(config.bitRate));
        mRgCamera.check(config.cameraType == Camera.CameraInfo.CAMERA_FACING_BACK ?
                R.id.rb_back_camera : R.id.rb_front_camera);
        mRgOrientation.check(config.orientation == Config.Orientation.HORIZONTAL ?
                R.id.rb_horizontal : R.id.rb_vertical);

        mRgEncode.check(config.useSofeEncode ?
                R.id.rb_softencode : R.id.rb_hardencode);
        if (config.resolution == Config.Resolution.HIGH) {
            mRgResolution.check(R.id.rb_high);
        } else if (config.resolution == Config.Resolution.NORMAL) {
            mRgResolution.check(R.id.rb_normal);
        } else if (config.resolution == Config.Resolution.LOW) {
            mRgResolution.check(R.id.rb_low);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_high:
                config.resolution = Config.Resolution.HIGH;
                break;
            case R.id.rb_normal:
                config.resolution = Config.Resolution.NORMAL;
                break;
            case R.id.rb_low:
                config.resolution = Config.Resolution.LOW;
                break;
            case R.id.rb_front_camera:
                config.cameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
                break;
            case R.id.rb_back_camera:
                config.cameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
                break;
            case R.id.rb_horizontal:
                config.orientation = Config.Orientation.HORIZONTAL;
                break;
            case R.id.rb_vertical:
                config.orientation = Config.Orientation.VERTICAL;
                break;
            case R.id.rb_softencode:
                config.useSofeEncode = true;
                break;
            case R.id.rb_hardencode:
                config.useSofeEncode = false;
                break;

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_save:
                config.fps = Integer.parseInt(mEtFps.getText().toString());
                config.bitRate = Integer.parseInt(mEtBitrate.getText().toString());
                config.url = mEtUrl.getText().toString();
                //Log.e(TAG, resolution.name());
                MyApplication.getInstance().config = config;
//                Toast.makeText(this, "save setting", Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
    }
}
