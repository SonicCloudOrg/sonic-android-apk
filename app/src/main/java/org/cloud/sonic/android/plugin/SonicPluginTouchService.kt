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
package org.cloud.sonic.android.plugin

import android.graphics.Point
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import org.cloud.sonic.android.plugin.touchPlugin.touchCompat.InputManagerWrapper
import org.cloud.sonic.android.plugin.touchPlugin.touchCompat.WindowManagerWrapper
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class SonicPluginTouchService(var width: Int = 0, var handler: Handler?) :
  Thread() {
  private val TAG = "SonicPluginTouchService"

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      try {
        Looper.prepare()
        val handler = Handler()
        val size: Point? = getScreenSize()
        if (size != null) {
          val m: SonicPluginTouchService =
            SonicPluginTouchService(
              size.x,
              handler
            )
          m.start()
          println("starting：start()")
          Looper.loop()
        } else {
          System.err.println("Couldn't get screen resolution")
          System.exit(1)
        }
      } catch (e: Exception) {
        System.err.println(e.message)
      }

    }

    fun getInstance(className: String): Any {
      val aClass = Class.forName(className)
      val getInstance = aClass.getMethod("getInstance")
      return getInstance.invoke(null)
    }

    fun getScreenSize(): Point? {
      val displayManager: Any =
        getInstance("android.hardware.display.DisplayManagerGlobal")
      try {
        val displayInfo =
          displayManager.javaClass.getMethod("getDisplayInfo", Int::class.javaPrimitiveType)
            .invoke(displayManager, Display.DEFAULT_DISPLAY)
        if (displayInfo != null) {
          val cls: Class<*> = displayInfo.javaClass
          val width = cls.getDeclaredField("logicalWidth").getInt(displayInfo)
          val height = cls.getDeclaredField("logicalHeight").getInt(displayInfo)
          return Point(width, height)
        }
      } catch (e: IllegalAccessException) {
        e.printStackTrace()
      } catch (e: InvocationTargetException) {
        e.printStackTrace()
      } catch (e: NoSuchMethodException) {
        e.printStackTrace()
      } catch (e: NoSuchFieldException) {
        e.printStackTrace()
      }
      return null
    }
  }

  private val SOCKET = "sonictouchservice"
  private val DEFAULT_MAX_CONTACTS = 10
  private val DEFAULT_MAX_PRESSURE = 0

  private var serverSocket: LocalServerSocket? = null

  private val pointerProperties = arrayOfNulls<PointerProperties>(2)
  private val pointerCoords = arrayOfNulls<PointerCoords>(2)
  private val events: Array<PointerEvent?> = arrayOfNulls<PointerEvent>(2)

  private var inputManager: InputManagerWrapper? = null
  private var windowManager: WindowManagerWrapper? = null
  private var hasStop = false

  init {
    inputManager = InputManagerWrapper()
    windowManager = WindowManagerWrapper()
    val pointerProps0 = PointerProperties()
    pointerProps0.id = 0
    pointerProps0.toolType = MotionEvent.TOOL_TYPE_FINGER
    val pointerProps1 = PointerProperties()
    pointerProps1.id = 1
    pointerProps1.toolType = MotionEvent.TOOL_TYPE_FINGER
    pointerProperties[0] = pointerProps0
    pointerProperties[1] = pointerProps1
    val pointerCoords0 = PointerCoords()
    val pointerCoords1 = PointerCoords()
    pointerCoords0.orientation = 0f
    pointerCoords0.pressure = 1f
    pointerCoords0.size = 1f
    pointerCoords1.orientation = 0f
    pointerCoords1.pressure = 1f
    pointerCoords1.size = 1f
    pointerCoords[0] = pointerCoords0
    pointerCoords[1] = pointerCoords1
    events[0] = PointerEvent()
    events[1] = PointerEvent()
  }

  private class PointerEvent {
    var lastMouseDown: Long = 0
    var lastX = 0
    var lastY = 0
    var action = 0
  }


  private fun injectEvent(event: InputEvent) {
    handler?.post(Runnable { inputManager?.injectInputEvent(event) })
  }

  private fun getMotionEvent(p: PointerEvent): MotionEvent? {
    return getMotionEvent(p, 0)
  }

  private fun getMotionEvent(
    p: PointerEvent,
    idx: Int
  ): MotionEvent? {
    val now = SystemClock.uptimeMillis()
    if (p.action == MotionEvent.ACTION_DOWN) {
      p.lastMouseDown = now
    }
    val coords = pointerCoords[idx]!!
    val rotation: Int = windowManager!!.getRotation()
    val rad = Math.toRadians(rotation * 90.0)
    coords.x = (p.lastX * Math.cos(-rad) - p.lastY * Math.sin(-rad)).toFloat()
    coords.y = (rotation * width + (p.lastX * Math.sin(-rad) + p.lastY * Math.cos(-rad))).toFloat()
    return MotionEvent.obtain(
      p.lastMouseDown, now, p.action, idx + 1, pointerProperties,
      pointerCoords, 0, 0, 1f, 1f, 0, 0,
      InputDevice.SOURCE_TOUCHSCREEN, 0
    )
  }

  private fun getMotionEvent(
    p1: PointerEvent,
    p2: PointerEvent
  ): List<MotionEvent?>? {
    val combinedEvents: MutableList<MotionEvent?> = ArrayList(2)
    val now = SystemClock.uptimeMillis()
    if (p1.action != MotionEvent.ACTION_MOVE) {
      combinedEvents.add(getMotionEvent(p1))
      combinedEvents.add(getMotionEvent(p2, 1))
    } else {
      val coords1 = pointerCoords[0]!!
      val coords2 = pointerCoords[1]!!
      val rotation: Int = windowManager!!.getRotation()
      val rad = Math.toRadians(rotation * 90.0)
      coords1.x = (p1.lastX * cos(-rad) - p1.lastY * sin(-rad)).toFloat()
      coords1.y =
        rotation * width + (p1.lastX * sin(-rad) + p1.lastY * cos(-rad)) as Float
      coords2.x = (p2.lastX * cos(-rad) - p2.lastY * sin(-rad)).toFloat()
      coords2.y =
        rotation * width + (p2.lastX * sin(-rad) + p2.lastY * cos(-rad)) as Float
      val event = MotionEvent.obtain(
        p1.lastMouseDown, now, p1.action, 2, pointerProperties,
        pointerCoords, 0, 0, 1f, 1f, 0, 0,
        InputDevice.SOURCE_TOUCHSCREEN, 0
      )
      combinedEvents.add(event)
    }
    return combinedEvents
  }

  private fun manageClientConnection() {
    Log.i(TAG, String.format("Listening on %s", SOCKET))
    var clientSocket: LocalSocket?
    try {
      clientSocket = serverSocket!!.accept()
      Log.i(TAG, "client connected")
      processCommandLoop(clientSocket)
    } catch (e: IOException) {
      Log.i(TAG, "error")
    }
  }

  @Throws(IOException::class)
  private fun processCommandLoop(clientSocket: LocalSocket) {
    BufferedReader(InputStreamReader(clientSocket.inputStream)).use { `in` ->
      var cmd: String?
      var count = 0
      do {
        cmd = `in`.readLine()
        if (cmd == null) {
          break
        }
        try {
          Scanner(cmd).use { scanner ->
            scanner.useDelimiter(" ")
            val type = scanner.next()
            val contact: Int
            when (type) {
              "u" -> {
                count++
                contact = scanner.nextInt()
                events[contact]!!.action =
                  if (contact == 0) MotionEvent.ACTION_UP else MotionEvent.ACTION_POINTER_2_UP
              }
              "d" -> {
                count++
                contact = scanner.nextInt()
                events[contact]!!.lastX = scanner.nextInt()
                events[contact]!!.lastY = scanner.nextInt()
                events[contact]!!.action =
                  if (contact == 0) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_POINTER_2_DOWN
              }
              "m" -> {
                count++
                contact = scanner.nextInt()
                events[contact]!!.lastX = scanner.nextInt()
                events[contact]!!.lastY = scanner.nextInt()
                events[contact]!!.action = MotionEvent.ACTION_MOVE
              }
              "r" -> hasStop = true
              else -> println("could not parse: $cmd")
            }
            if (count == 1) {
              injectEvent(getMotionEvent(events[0]!!)!!)
            } else if (count == 2) {
              for (event in getMotionEvent(
                events[0]!!,
                events[1]!!
              )!!) {
                injectEvent(event!!)
              }
            } else {
              println("count not manage events #$count")
            }
            count = 0
          }
        } catch (e: NoSuchElementException) {
          println("could not parse: $cmd")
        } catch (e: InterruptedException) {
          e.printStackTrace()
        }
      } while (!hasStop)
    }
  }


  override fun run() {

    try {
      Log.i(TAG, String.format("creating socket %s", SOCKET))
      serverSocket = LocalServerSocket(SOCKET)
    } catch (e: IOException) {
      println(e.message)
      e.printStackTrace()
      return
    }

    manageClientConnection()

    try {
      serverSocket?.close()
    } catch (e: IOException) {
      println(e.message)
      e.printStackTrace()
    }
    Log.i(TAG, "socket closed.")
    System.exit(0)
  }

}
