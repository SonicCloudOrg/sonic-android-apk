package org.cloud.sonic.android.plugin.touchPlugin.touch;

import android.view.InputEvent;
import android.view.KeyEvent;

import org.cloud.sonic.android.utils.InternalApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * More https://github.com/DeviceFarmer/STFService.apk
 */
public class InputManagerWrapper {
    private EventInjector eventInjector;

    public InputManagerWrapper() {
        try {
            eventInjector = new InputManagerEventInjector();
        } catch (UnsupportedOperationException e) {
            eventInjector = new WindowManagerEventInjector();
        }
    }

    public boolean injectInputEvent(InputEvent event) {
        return eventInjector.injectInputEvent(event);
    }

    private interface EventInjector {
        boolean injectInputEvent(InputEvent event);
    }

    private class InputManagerEventInjector implements EventInjector {
        public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
        private Object inputManager;
        private Method injector;

        public InputManagerEventInjector() {
            try {
                inputManager = InternalApi.getSingleton("android.hardware.input.InputManager");

                injector = inputManager.getClass()
                    .getMethod("injectInputEvent", InputEvent.class, int.class);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException("InputManagerEventInjector is not supported in this device! " +
                    "Please submit your deviceInfo to https://github.com/SonicCloudOrg/sonic-android-apk");
            }
        }

        @Override
        public boolean injectInputEvent(InputEvent event) {
            try {
                injector.invoke(inputManager, event, INJECT_INPUT_EVENT_MODE_ASYNC);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private class WindowManagerEventInjector implements EventInjector {
        private Object windowManager;
        private Method keyInjector;

        public WindowManagerEventInjector() {
            try {
                windowManager = WindowManagerWrapper.getWindowManager();

                keyInjector = windowManager.getClass()
                    .getMethod("injectKeyEvent", KeyEvent.class, boolean.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException("WindowManagerEventInjector is not supported in this device!" +
                    " Please submit your deviceInfo to https://github.com/SonicCloudOrg/sonic-android-apk");
            }
        }

        @Override
        public boolean injectInputEvent(InputEvent event) {
            return false;
        }
    }
}
