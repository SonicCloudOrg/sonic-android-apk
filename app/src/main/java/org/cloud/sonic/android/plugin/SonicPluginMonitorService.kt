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

import org.cloud.sonic.android.plugin.touchPlugin.touchCompat.WindowManagerWrapper

class SonicPluginMonitorService : Thread() {
  private val wmw: WindowManagerWrapper = WindowManagerWrapper()
  private val lock = java.lang.Object()

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      try {
        val monitor: SonicPluginMonitorService =
          SonicPluginMonitorService()
        monitor.start()
        monitor.join()
      } catch (e: InterruptedException) {
        println("ERROR:${e.message}")
      }
    }
  }

  override fun run() {
    val watcher = object : WindowManagerWrapper.RotationWatcher {
      override fun onRotationChanged(rotation: Int) {
        println(rotation)
      }
    }
    try {
      println(wmw.getRotation())
      wmw.watchRotation(watcher)
      synchronized(lock) {
        while (!isInterrupted) {
          lock.wait()
        }
      }
    } catch (e: InterruptedException) {
      println("ERROR:${e.message}")
    }

  }
}
