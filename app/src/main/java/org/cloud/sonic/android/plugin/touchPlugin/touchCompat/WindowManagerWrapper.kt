package org.cloud.sonic.android.plugin.touchPlugin.touchCompat

import android.os.IBinder
import android.os.RemoteException
import android.view.IRotationWatcher
import com.blankj.utilcode.util.ReflectUtils
import java.lang.reflect.InvocationTargetException

class WindowManagerWrapper {
    private var windowManager: Any? = null

    companion object{
        fun getWindowManager(): Any? {
            val serviceManager = ReflectUtils.reflect("android.os.ServiceManager").method("getService",String::class.java)
            val tub = ReflectUtils.reflect("android.view.IWindowManager\$Stub").method("asInterface",IBinder::class.java,serviceManager)
            return tub
        }

    }

    init {
        windowManager = WindowManagerWrapper.getWindowManager()
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


    fun watchRotation(watcher:RotationWatcher):Any{
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