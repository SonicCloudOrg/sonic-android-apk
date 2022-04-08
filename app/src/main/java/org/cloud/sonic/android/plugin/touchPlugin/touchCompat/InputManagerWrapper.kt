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

    class InputManagerEventInjector:EventInjector{
        val INJECT_INPUT_EVENT_MODE_ASYNC = 0
        private var inputManager: Any? = null
        private var injector: Method? = null

        init {
            try {
                inputManager = ReflectUtils.reflect("android.hardware.input.InputManager").method("getInstance")
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

    class WindowManagerEventInjector:EventInjector{
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