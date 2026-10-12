package com.local.swipepilot;

import android.util.Log;

/** Stable lifecycle signals only: never log page text, target labels or exception messages. */
final class ServiceDiagnostics {
    private static final String TAG = "SwipePilot";

    private ServiceDiagnostics() {}

    static void event(String event) {
        Log.i(TAG, event);
    }

    static void stepFailed(Exception error) {
        Log.w(TAG, "step_failed type=" + error.getClass().getName());
    }
}
