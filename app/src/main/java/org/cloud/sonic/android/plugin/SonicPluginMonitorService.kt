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