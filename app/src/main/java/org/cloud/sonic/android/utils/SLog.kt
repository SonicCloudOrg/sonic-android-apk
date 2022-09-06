/*
 *
 * Copyright (C) [SonicCloudOrg] Sonic Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.cloud.sonic.android.utils

import android.util.Log
import org.cloud.sonic.android.utils.SLog
import kotlin.jvm.JvmOverloads

object SLog {
    private const val TAG = "SonicLog"
    private const val PREFIX = "SystemLog "
    private var mIsDebug = false

    fun initLog(isLog:Boolean) {
        mIsDebug = isLog
    }

    private fun isDebug(): Boolean {
        return mIsDebug
    }

    fun v(message: String) {
        if (isDebug()) {
            Log.v(TAG, message)
            println(PREFIX + "VERBOSE: " + message)
        }
    }

    fun d(message: String) {
        if (isDebug()) {
            Log.d(TAG, message)
            println(PREFIX + "DEBUG: " + message)
        }
    }

    fun i(message: String) {
        Log.i(TAG, message)
        println(PREFIX + "INFO: " + message)
    }

    @JvmOverloads
    fun w(message: String, throwable: Throwable? = null) {
        if (isDebug()) {
            Log.w(TAG, message, throwable)
            println(PREFIX + "WARN: " + message)
            throwable?.printStackTrace()
        }
    }

    @JvmOverloads
    fun e(message: String, throwable: Throwable? = null) {
         Log.e(TAG, message, throwable)
         println(PREFIX + "ERROR: " + message)
         throwable?.printStackTrace()
    }

    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
}