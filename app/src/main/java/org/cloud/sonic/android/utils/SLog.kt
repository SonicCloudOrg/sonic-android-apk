/*
 *  sonic-android-apk  Help your Android device to do more.
 *  Copyright (C) 2022 SonicCloudOrg
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
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