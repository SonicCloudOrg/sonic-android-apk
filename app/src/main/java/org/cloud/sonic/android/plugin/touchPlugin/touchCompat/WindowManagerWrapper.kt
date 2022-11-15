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
package org.cloud.sonic.android.plugin.touchPlugin.touchCompat

import android.os.IBinder
import android.os.RemoteException
import android.view.IRotationWatcher
import java.lang.reflect.InvocationTargetException

/**
 * @see https://github.com/openstf/STFService.apk/blob/master/app/src/main/java/jp/co/cyberagent/stf/compat/WindowManagerWrapper.java
 */
class WindowManagerWrapper {
    private var windowManager: Any? = null

    init {
        windowManager = getWindowManager()
    }

    companion object {
        private fun getBinder(name: String): Any {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val getService = serviceManager.getMethod(
                "getService",
                String::class.java
            )
            return getService.invoke(null, name)
        }

        private fun getService(serviceName: String, interfaceClass: String): Any {
            val serviceBinder = getBinder(serviceName)
            val s = Class.forName(interfaceClass)
            val asInterface = s.getMethod("asInterface", IBinder::class.java)
            return asInterface.invoke(null, serviceBinder)
        }

        fun getWindowManager(): Any? {
            return getService("window", "android.view.IWindowManager\$Stub")
        }

    }

    fun watchRotation(watcher: RotationWatcher): Any {
        val realWatcher: IRotationWatcher = object : IRotationWatcher.Stub() {
            @Throws(RemoteException::class)
            override fun onRotationChanged(rotation: Int) {
                watcher.onRotationChanged(rotation)
            }
        }

        return try {
            val getter = windowManager!!.javaClass.getMethod(
                "watchRotation",
                IRotationWatcher::class.java,
                Int::class.javaPrimitiveType
            )
            getter.invoke(windowManager, realWatcher, 0)
            realWatcher
        } catch (e: NoSuchMethodException) {
            try {
                val getter = windowManager!!.javaClass.getMethod(
                    "watchRotation",
                    IRotationWatcher::class.java
                )
                getter.invoke(windowManager, realWatcher)
                realWatcher
            } catch (e2: java.lang.Exception) {
                throw UnsupportedOperationException("UnsupportedOperationException: " + e2.message)
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    fun getRotation(): Int {
        try {
            val getter = windowManager!!.javaClass.getMethod("getDefaultDisplayRotation")
            return getter.invoke(windowManager) as Int
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        try {
            val getter = windowManager!!.javaClass.getMethod("getRotation")
            return getter.invoke(windowManager) as Int
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    interface RotationWatcher {
        fun onRotationChanged(rotation: Int)
    }
}
