/*
 *  Copyright (C) [SonicCloudOrg] Sonic Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.cloud.sonic.android.plugin.touchPlugin.touchCompat

import android.os.IBinder
import android.os.RemoteException
import android.view.IRotationWatcher
import java.lang.reflect.InvocationTargetException

class WindowManagerWrapper {
  private var windowManager: Any? = null

  companion object {
    fun getBinder(name: String): Any {
      val ServiceManager = Class.forName("android.os.ServiceManager")
      val getService = ServiceManager.getMethod(
        "getService",
        String::class.java
      )
      return getService.invoke(null, name)
    }

    fun getService(serviceName: String, interfaceClass: String): Any {
      val serviceBinder = getBinder(serviceName)
      val Stub = Class.forName(interfaceClass)
      val asInterface = Stub.getMethod("asInterface", IBinder::class.java)
      return asInterface.invoke(null, serviceBinder)
    }

    fun getWindowManager(): Any? {
      return getService("window", "android.view.IWindowManager\$Stub")
    }

  }

  init {
    windowManager = getWindowManager()
  }

  fun getRotation(): Int {
    try {
      val getter = windowManager!!.javaClass.getMethod("getDefaultDisplayRotation")
      return getter.invoke(windowManager) as Int
    } catch (e: NoSuchMethodException) {
    } catch (e: IllegalAccessException) {
      e.printStackTrace()
    } catch (e: InvocationTargetException) {
      e.printStackTrace()
    }
    try {
      val getter = windowManager!!.javaClass.getMethod("getRotation")
      return getter.invoke(windowManager) as Int
    } catch (e: NoSuchMethodException) {
      e.printStackTrace()
    } catch (e: IllegalAccessException) {
      e.printStackTrace()
    } catch (e: InvocationTargetException) {
      e.printStackTrace()
    }
    return 0
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
      } catch (e2: NoSuchMethodException) {
        throw UnsupportedOperationException("watchRotation is not supported: " + e2.message)
      } catch (e2: IllegalAccessException) {
        throw UnsupportedOperationException("watchRotation is not supported: " + e2.message)
      } catch (e2: InvocationTargetException) {
        throw UnsupportedOperationException("watchRotation is not supported: " + e2.message)
      }
    } catch (e: IllegalAccessException) {
      throw UnsupportedOperationException("watchRotation is not supported: " + e.message)
    } catch (e: InvocationTargetException) {
      throw UnsupportedOperationException("watchRotation is not supported: " + e.message)
    }
  }

  interface RotationWatcher {
    fun onRotationChanged(rotation: Int)
  }
}
