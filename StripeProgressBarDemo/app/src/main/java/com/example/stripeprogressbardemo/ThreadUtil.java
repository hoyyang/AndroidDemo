package com.example.stripeprogressbardemo;

import android.os.Looper;

public class ThreadUtil {
    public static void checkRunInMainThread(String exceptionMessage) {
        if (!Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            throw new IllegalStateException(exceptionMessage);
        }
    }
}
