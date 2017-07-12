package com.seu.magiccamera.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.seu.magiccamera.adapter.FilterAdapter;
import com.seu.magicfilter.MagicEngine;
import com.seu.magicfilter.filter.helper.MagicFilterType;
import com.seu.magicfilter.utils.MagicParams;
import com.seu.magicfilter.widget.MagicCameraView;
import com.seu.magicfilterdemo.R;
import com.upyun.hardware.Watermark;
import com.upyun.hardware.WatermarkPosition;

public class CameraActivity extends Activity {
    private LinearLayout mFilterLayout;
    private RecyclerView mFilterListView;
    private FilterAdapter mAdapter;
    private MagicEngine magicEngine;
    private boolean isRecording = false;
    private ImageView btn_shutter;
    private ImageView btn_flashlight;

    private ObjectAnimator animator;

    MagicCameraView cameraView;

    private final MagicFilterType[] types = new MagicFilterType[]{
            MagicFilterType.NONE,
            MagicFilterType.FAIRYTALE,
            MagicFilterType.SUNRISE,
            MagicFilterType.SUNSET,
            MagicFilterType.WHITECAT,
            MagicFilterType.BLACKCAT,
            MagicFilterType.SKINWHITEN,
            MagicFilterType.HEALTHY,
            MagicFilterType.SWEETS,
            MagicFilterType.ROMANCE,
            MagicFilterType.SAKURA,
            MagicFilterType.WARM,
            MagicFilterType.ANTIQUE,
            MagicFilterType.NOSTALGIA,
            MagicFilterType.CALM,
            MagicFilterType.LATTE,
            MagicFilterType.TENDER,
            MagicFilterType.COOL,
            MagicFilterType.EMERALD,
            MagicFilterType.EVERGREEN,
            MagicFilterType.CRAYON,
            MagicFilterType.SKETCH,
            MagicFilterType.AMARO,
            MagicFilterType.BRANNAN,
            MagicFilterType.BROOKLYN,
            MagicFilterType.EARLYBIRD,
            MagicFilterType.FREUD,
            MagicFilterType.HEFE,
            MagicFilterType.HUDSON,
            MagicFilterType.INKWELL,
            MagicFilterType.KEVIN,
            MagicFilterType.LOMO,
            MagicFilterType.N1977,
            MagicFilterType.NASHVILLE,
            MagicFilterType.PIXAR,
            MagicFilterType.RISE,
            MagicFilterType.SIERRA,
            MagicFilterType.SUTRO,
            MagicFilterType.TOASTER2,
            MagicFilterType.VALENCIA,
            MagicFilterType.WALDEN,
            MagicFilterType.XPROII
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        MagicEngine.Builder builder = new MagicEngine.Builder();
        magicEngine = builder
                .build((MagicCameraView) findViewById(R.id.glsurfaceview_camera));
        initView();

        magicEngine.startRecord();
        animator.start();
        isRecording = true;

//        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//            @Override
//            public void uncaughtException(Thread thread, final Throwable ex) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.e("推流失败：", ex.toString());
//                        animator.end();
//                        magicEngine.stopRecord();
//                        isRecording = false;
//                    }
//                });
//            }
//        });
    }

    private void initView() {
        mFilterLayout = (LinearLayout) findViewById(R.id.layout_filter);
        mFilterListView = (RecyclerView) findViewById(R.id.filter_listView);

        btn_shutter = (ImageView) findViewById(R.id.btn_camera_shutter);
        findViewById(R.id.btn_camera_filter).setOnClickListener(btn_listener);
        findViewById(R.id.btn_camera_closefilter).setOnClickListener(btn_listener);
        findViewById(R.id.btn_camera_shutter).setOnClickListener(btn_listener);
        findViewById(R.id.btn_camera_switch).setOnClickListener(btn_listener);
        findViewById(R.id.btn_camera_beauty).setOnClickListener(btn_listener);

        btn_flashlight = (ImageView) findViewById(R.id.btn_flashlight_switch);
        btn_flashlight.setOnClickListener(btn_listener);
        btn_flashlight.setEnabled(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mFilterListView.setLayoutManager(linearLayoutManager);

        mAdapter = new FilterAdapter(this, types);
        mFilterListView.setAdapter(mAdapter);
        mAdapter.setOnFilterChangeListener(onFilterChangeListener);

        animator = ObjectAnimator.ofFloat(btn_shutter, "rotation", 0, 360);
        animator.setDuration(500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        cameraView = (MagicCameraView) findViewById(R.id.glsurfaceview_camera);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) cameraView.getLayoutParams();
        params.width = screenSize.x;
        params.height = screenSize.x * 4 / 3;
        cameraView.setLayoutParams(params);
        cameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //magicEngine.focusOnTouch();
                }
                return true;
            }
        });

        //设置水印
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.watermark);
//        Watermark watermark = new Watermark(bitmap, 100, 50, WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT, 8, 8);
//        cameraView.setWatermark(watermark);
    }

    private FilterAdapter.onFilterChangeListener onFilterChangeListener = new FilterAdapter.onFilterChangeListener() {

        @Override
        public void onFilterChanged(MagicFilterType filterType) {
            magicEngine.setFilter(filterType);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (grantResults.length != 1 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pushVideo();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private View.OnClickListener btn_listener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_camera_shutter:
                    if (PermissionChecker.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                v.getId());
                    } else {
                        pushVideo();
                    }
                    break;
                case R.id.btn_camera_filter:
                    showFilters();
                    break;
                case R.id.btn_camera_switch:
//                    magicEngine.switchCamera();
                    cameraView.switchCamera();
                    btn_flashlight.setEnabled(!btn_flashlight.isEnabled());
                    break;
                case R.id.btn_camera_beauty:
                    new AlertDialog.Builder(CameraActivity.this)
                            .setSingleChoiceItems(new String[]{"关闭", "1", "2", "3", "4", "5"}, MagicParams.beautyLevel,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            magicEngine.setBeautyLevel(which);
                                            dialog.dismiss();
                                        }
                                    })
                            .setNegativeButton("取消", null)
                            .show();
                    break;
                case R.id.btn_camera_closefilter:
                    hideFilters();
                    break;
                case R.id.btn_flashlight_switch:
                    magicEngine.switchFlashlight();
                    break;
            }
        }
    };

    private void pushVideo() {
        if (isRecording) {
            animator.end();
            magicEngine.stopRecord();
        } else {
            animator.start();
            magicEngine.startRecord();
        }
        isRecording = !isRecording;
    }

    private void showFilters() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(mFilterLayout, "translationY", mFilterLayout.getHeight(), 0);
        animator.setDuration(200);
        animator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                findViewById(R.id.btn_camera_shutter).setClickable(false);
                mFilterLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }
        });
        animator.start();
    }

    private void hideFilters() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(mFilterLayout, "translationY", 0, mFilterLayout.getHeight());
        animator.setDuration(200);
        animator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // TODO Auto-generated method stub
                mFilterLayout.setVisibility(View.INVISIBLE);
                findViewById(R.id.btn_camera_shutter).setClickable(true);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // TODO Auto-generated method stub
                mFilterLayout.setVisibility(View.INVISIBLE);
                findViewById(R.id.btn_camera_shutter).setClickable(true);
            }
        });
        animator.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        magicEngine.stopRecord();
    }
}
