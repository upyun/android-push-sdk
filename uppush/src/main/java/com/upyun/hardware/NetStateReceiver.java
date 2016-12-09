package com.upyun.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

//网络状态监听类
public class NetStateReceiver extends BroadcastReceiver {
    private static int netWorkType = UConstant.NET_WORK_NULL;
    private NetStateHandler mHandler = null;

    public NetStateReceiver(NetStateHandler handler) {
        this.mHandler = handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            netWorkType = isNetworkAvailable(context);
            if (netWorkType == UConstant.NET_WORK_NULL) {
                Toast.makeText(context, "网络连接已断开!", Toast.LENGTH_LONG).show();
            } else if (netWorkType == UConstant.NET_WORK_WIFI) {
                Toast.makeText(context, "wifi已连接!",Toast.LENGTH_LONG).show();
            } else if (netWorkType == UConstant.NET_WORK_MOBILE) {
                Toast.makeText(context, "普通网络已连接!",Toast.LENGTH_LONG).show();
            }
            if (mHandler != null) {
                mHandler.onNetStateChanged(netWorkType);
            }
        }
    }

    //检测网络可用性
    private int isNetworkAvailable(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMobile = mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mWifi != null && mWifi.isAvailable()) {
            if (mWifi.isConnected()) {
                return UConstant.NET_WORK_WIFI;
            }
        } else if (mMobile != null && mMobile.isAvailable()) {
            if (mMobile.isConnected()) {
                return UConstant.NET_WORK_MOBILE;
            }
        }

        return UConstant.NET_WORK_NULL;
    }

    //返回当前网络状态
    public static int getNetWorkStatus() {
        return netWorkType;
    }

    /**
     * Network state handler.
     */
    public interface NetStateHandler {
        void onNetStateChanged(int state);
    }
}