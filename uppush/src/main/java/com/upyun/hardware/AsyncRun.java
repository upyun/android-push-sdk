package com.upyun.hardware;

import android.os.Handler;
import android.os.Looper;

public class AsyncRun {
    public static void run(Runnable r) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(r);
    }

    public static void run(Runnable r, int deley) {
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(r, deley);
    }
}