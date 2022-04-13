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

import org.cloud.sonic.android.plugin.touchPlugin.touchCompat.WindowManagerWrapper

class SonicPluginMonitorService:Thread() {
    private val wmw: WindowManagerWrapper = WindowManagerWrapper()
    private val lock = java.lang.Object()
    companion object{
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
            synchronized(this) {
                while (!isInterrupted) {
                    lock.wait()
                }
            }
        } catch (e: InterruptedException) {
            println("ERROR:${e.message}")
        }

    }
}
