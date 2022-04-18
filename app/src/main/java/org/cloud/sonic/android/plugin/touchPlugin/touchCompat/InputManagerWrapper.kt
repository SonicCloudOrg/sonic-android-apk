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

import android.view.InputEvent
import android.view.KeyEvent
import com.blankj.utilcode.util.ReflectUtils
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class InputManagerWrapper {
  private lateinit var eventInjector: EventInjector

  init {
    eventInjector = try {
      InputManagerEventInjector()
    } catch (e: UnsupportedOperationException) {
      WindowManagerEventInjector()
    }
  }


  fun injectInputEvent(event: InputEvent?): Boolean {
    return eventInjector.injectInputEvent(event)
  }

  private interface EventInjector {
    fun injectInputEvent(event: InputEvent?): Boolean
  }

  class InputManagerEventInjector : EventInjector {
    val INJECT_INPUT_EVENT_MODE_ASYNC = 0
    private var inputManager: Any? = null
    private var injector: Method? = null

    fun getInstance(className: String): Any {
      val aClass = Class.forName(className)
      val getInstance = aClass.getMethod("getInstance")
      return getInstance.invoke(null)
    }

    init {
      try {
        inputManager = getInstance("android.hardware.input.InputManager")
        injector = inputManager?.javaClass
          ?.getMethod(
            "injectInputEvent",
            InputEvent::class.java,
            Int::class.javaPrimitiveType
          )
      } catch (e: NoSuchMethodException) {
        throw java.lang.UnsupportedOperationException(
          "InputManagerEventInjector is not supported in this device! " +
            "Please submit your deviceInfo to https://github.com/SonicCloudOrg/sonic-android-apk"
        )
      }
    }

    override fun injectInputEvent(event: InputEvent?): Boolean {
      return try {
        injector!!.invoke(
          inputManager,
          event,
          INJECT_INPUT_EVENT_MODE_ASYNC
        )
        true
      } catch (e: IllegalAccessException) {
        e.printStackTrace()
        false
      } catch (e: InvocationTargetException) {
        e.printStackTrace()
        false
      }
    }
  }

  class WindowManagerEventInjector : EventInjector {
    private var windowManager: Any? = null
    private var keyInjector: Method? = null

    init {
      try {
        windowManager = WindowManagerWrapper.getWindowManager()
        keyInjector = windowManager?.javaClass
          ?.getMethod(
            "injectKeyEvent",
            KeyEvent::class.java,
            Boolean::class.javaPrimitiveType
          )
      } catch (e: NoSuchMethodException) {
        e.printStackTrace()
        throw java.lang.UnsupportedOperationException(
          "WindowManagerEventInjector is not supported in this device!" +
            " Please submit your deviceInfo to https://github.com/SonicCloudOrg/sonic-android-apk"
        )
      }
    }

    override fun injectInputEvent(event: InputEvent?): Boolean {
      return false
    }

  }
}
