package com.example.syncro.utils;

import android.util.Log;

import com.example.syncro.BuildConfig;

public class Logger {
    private static final boolean DEBUG = BuildConfig.DEBUG; // false en release

    public static void d(String tag, String msg) {
        if (DEBUG) Log.d(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (DEBUG) Log.e(tag, msg);
    }
}
