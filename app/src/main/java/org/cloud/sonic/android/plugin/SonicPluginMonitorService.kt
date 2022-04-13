package org.cloud.sonic.android.plugin

import org.cloud.sonic.android.plugin.touchPlugin.touch.WindowManagerWrapper

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
        val watcher =
            WindowManagerWrapper.RotationWatcher { rotation -> println(rotation) }
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
