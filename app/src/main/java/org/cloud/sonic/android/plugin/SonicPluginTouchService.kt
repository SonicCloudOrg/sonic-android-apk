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
package org.cloud.sonic.android.plugin

import android.graphics.Point
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import org.cloud.sonic.android.plugin.controllerPlugin.Event
import org.cloud.sonic.android.plugin.touchPlugin.touchCompat.InputManagerWrapper
import org.cloud.sonic.android.plugin.touchPlugin.touchCompat.WindowManagerWrapper
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.system.exitProcess

class SonicPluginTouchService(var width: Int = 0, var handler: Handler?) :
    Thread() {
    private val TAG = "SonicPluginTouchService"
    private val SOCKET = "sonictouchservice"
    private val DEFAULT_MAX_CONTACTS = 10
    val LINK_SOCKET_TIMEOUT = 30 * 1000

    private var serverSocket: LocalServerSocket? = null

    private val properties = arrayOfNulls<PointerProperties>(1)
    private val pointer = arrayOfNulls<PointerCoords>(1)
    private val events: Array<Event?> = arrayOfNulls(1)

    private var inputManager: InputManagerWrapper? = null
    private var windowManager: WindowManagerWrapper? = null
    private var hasStop = false

    init {
        inputManager = InputManagerWrapper()
        windowManager = WindowManagerWrapper()
        events[0] = Event()
        val props = PointerProperties()
        props.id = 0
        props.toolType = MotionEvent.TOOL_TYPE_FINGER
        properties[0] = props
        val coords = PointerCoords()
        coords.orientation = 0f
        coords.pressure = 1f
        coords.size = 1f
        pointer[0] = coords
    }

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
                    exitProcess(1)
                }
            } catch (e: Exception) {
                System.err.println(e.message)
            }
        }

        private fun getInstance(className: String): Any {
            val aClass = Class.forName(className)
            val getInstance = aClass.getMethod("getInstance")
            return getInstance.invoke(null)
        }

        private fun getScreenSize(): Point? {
            val displayManager: Any =
                getInstance("android.hardware.display.DisplayManagerGlobal")
            try {
                val displayInfo =
                    displayManager.javaClass.getMethod(
                        "getDisplayInfo",
                        Int::class.javaPrimitiveType
                    )
                        .invoke(displayManager, Display.DEFAULT_DISPLAY)
                if (displayInfo != null) {
                    val cls: Class<*> = displayInfo.javaClass
                    val width = cls.getDeclaredField("logicalWidth").getInt(displayInfo)
                    val height = cls.getDeclaredField("logicalHeight").getInt(displayInfo)
                    return Point(width, height)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    private fun getMotionEvent(p: Event): MotionEvent? {
        val rotation: Int = windowManager!!.getRotation()
        val now = SystemClock.uptimeMillis()
        if (p.action == 0) {
            p.down = now
        }
        val rad = Math.toRadians(rotation * 90.0)
        val coords = pointer[0]!!
        coords.x = (p.x * Math.cos(-rad) - p.y * Math.sin(-rad)).toFloat()
        coords.y =
            (rotation * width + (p.x * Math.sin(-rad) + p.y * Math.cos(-rad))).toFloat()
        return MotionEvent.obtain(
            p.down, now, p.action, 1, properties, pointer, 0, 0, 1f, 1f, 0, 0, 4098, 0
        )
    }

    @Throws(IOException::class)
    private fun processLoop(clientSocket: LocalSocket) {
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
                        when (type) {
                            "up" -> {
                                count++
                                events[0]!!.action = 1
                            }
                            "move" -> {
                                count++
                                events[0]!!.x = scanner.nextInt()
                                events[0]!!.y = scanner.nextInt()
                                events[0]!!.action = 2
                            }
                            "down" -> {
                                count++
                                events[0]!!.x = scanner.nextInt()
                                events[0]!!.y = scanner.nextInt()
                                events[0]!!.action = 0
                            }
                            "release" -> hasStop = true
                            else -> println("could not parse: $cmd")
                        }
                        if (count == 1) {
                            handler?.post { inputManager?.injectInputEvent(getMotionEvent(events[0]!!)!!) }
                        } else {
                            println("count not manage events #$count")
                        }
                        count = 0
                    }
                } catch (e: Exception) {
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

        Log.i(TAG, String.format("Listening on %s", SOCKET))
        var clientSocket: LocalSocket?
        try {
            clientSocket = serverSocket!!.accept()
            Log.i(TAG, "client connected")
            processLoop(clientSocket)
        } catch (e: IOException) {
            Log.i(TAG, "error")
        }

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
