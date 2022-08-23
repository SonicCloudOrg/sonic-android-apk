package org.cloud.sonic.android.lib.socketmanager.utils

import android.util.Log

object SonicSocketLog {
    var debug = true
    private const val TAG = "SonicSocket"
    fun debug(debug1: Boolean) {
        debug = debug1
    }

    fun d(tag: String, o: Any) {
        if (debug) {
            Log.d(TAG, "$tag $o")
        }
    }

    fun e(tag: String, o: Any) {
        if (debug) {
            Log.e(TAG, "$tag $o")
        }
    }
}