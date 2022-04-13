package org.cloud.sonic.android.plugin.touchPlugin.touch;

import android.os.RemoteException;
import android.view.IRotationWatcher;

import org.cloud.sonic.android.utils.InternalApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * More https://github.com/DeviceFarmer/STFService.apk
 */
public class WindowManagerWrapper {
    private Object windowManager;

    public static interface RotationWatcher {
        public void onRotationChanged(int rotation);
    }

    public WindowManagerWrapper() {
        windowManager = getWindowManager();
    }

    public int getRotation() {
        try {
            Method getter = windowManager.getClass().getMethod("getDefaultDisplayRotation");
            return (Integer) getter.invoke(windowManager);
        }
        catch (NoSuchMethodException e) {}
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        try {
            Method getter = windowManager.getClass().getMethod("getRotation");
            return (Integer) getter.invoke(windowManager);
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static Object getWindowManager() {
        return InternalApi.getServiceAsInterface("window", "android.view.IWindowManager$Stub");
    }

    public Object watchRotation(final RotationWatcher watcher) {
        IRotationWatcher realWatcher = new IRotationWatcher.Stub() {
            @Override
            public void onRotationChanged(int rotation) throws RemoteException {
                watcher.onRotationChanged(rotation);
            }
        };

        try {
            Method getter = windowManager.getClass().getMethod("watchRotation", IRotationWatcher.class, int.class);
            getter.invoke(windowManager, realWatcher, 0);
            return realWatcher;
        }
        catch (NoSuchMethodException e) {
            try {
                Method getter = windowManager.getClass().getMethod("watchRotation", IRotationWatcher.class);
                getter.invoke(windowManager, realWatcher);
                return realWatcher;
            }
            catch (NoSuchMethodException e2) {
                throw new UnsupportedOperationException("watchRotation is not supported: " + e2.getMessage());
            }
            catch (IllegalAccessException e2) {
                throw new UnsupportedOperationException("watchRotation is not supported: " + e2.getMessage());
            }
            catch (InvocationTargetException e2) {
                throw new UnsupportedOperationException("watchRotation is not supported: " + e2.getMessage());
            }
        }
        catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("watchRotation is not supported: " + e.getMessage());
        }
        catch (InvocationTargetException e) {
            throw new UnsupportedOperationException("watchRotation is not supported: " + e.getMessage());
        }
    }


}
