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

import android.view.InputEvent
import android.view.KeyEvent
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class InputManagerWrapper {
  private var eventInjector: EventInjector

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
    private val injectMode = 0
    private var inputManager: Any? = null
    private var injector: Method? = null

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

    private fun getInstance(className: String): Any {
      val aClass = Class.forName(className)
      val getInstance = aClass.getMethod("getInstance")
      return getInstance.invoke(null)
    }

    override fun injectInputEvent(event: InputEvent?): Boolean {
      return try {
        injector!!.invoke(
          inputManager,
          event,
          injectMode
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
