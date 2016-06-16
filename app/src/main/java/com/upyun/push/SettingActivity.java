package com.upyun.push;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.upyun.hardware.Config;

public class SettingActivity extends Activity implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {

    private static final String TAG = "SettingActivity";
    private EditText mEtUrl;
    private EditText mEtFps;
    private EditText mEtBitrate;
    private RadioGroup mRgResolution;
    private Button mBtSave;
    private Config.Resolution resolution;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mEtUrl = (EditText) findViewById(R.id.et_url);
        mEtFps = (EditText) findViewById(R.id.et_fps);
        mEtBitrate = (EditText) findViewById(R.id.et_bitrate);
        mRgResolution = (RadioGroup) findViewById(R.id.rg_resolution);
        mBtSave = (Button) findViewById(R.id.bt_save);

        mRgResolution.setOnCheckedChangeListener(this);
        mBtSave.setOnClickListener(this);
        resolution = Config.Resolution.NORMAL;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_high:
                resolution = Config.Resolution.HIGH;
                break;
            case R.id.rb_normal:
                resolution = Config.Resolution.NORMAL;
                break;
            case R.id.rb_low:
                resolution = Config.Resolution.LOW;
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_save:
                int fps = Integer.parseInt(mEtFps.getText().toString());
                int bitRate = Integer.parseInt(mEtBitrate.getText().toString());
                String url = mEtUrl.getText().toString();

                Log.e(TAG, resolution.name());

                MyApplication.getInstance().config = new Config.Builder().
                        fps(fps).
                        bitRate(bitRate).
                        URL(url).
                        resolutaion(resolution).
                        build();

                Toast.makeText(this, "save setting", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
