package org.cloud.sonic.android.utils;

import android.os.IBinder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @link{https://github.com/DeviceFarmer/STFService.apk}
 */
public class InternalApi {
    public static boolean hasService(String name) {
        try {
            Class<?> ServiceManager = Class.forName("android.os.ServiceManager");
            Method getService = ServiceManager.getMethod("getService", String.class);
            return getService.invoke(null, name) != null;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
        catch (NoSuchMethodException e) {
            return false;
        }
        catch (IllegalAccessException e) {
            return false;
        }
        catch (InvocationTargetException e) {
            return false;
        }
    }

    public static Object getServiceBinder(String name) {
        try {
            Class<?> ServiceManager = Class.forName("android.os.ServiceManager");
            Method getService = ServiceManager.getMethod("getService", String.class);
            return getService.invoke(null, name);
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getServiceAsInterface(String serviceName, String interfaceClass) {
        try {
            Object serviceBinder = getServiceBinder(serviceName);

            Class<?> Stub = Class.forName(interfaceClass);

            Method asInterface = Stub.getMethod("asInterface", IBinder.class);

            return asInterface.invoke(null, serviceBinder);
        }
        catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Unsupported service " + serviceName + ": " + e.getMessage());
        }
        catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Unsupported service " + serviceName + ": " + e.getMessage());
        }
        catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Unsupported service " + serviceName + ": " + e.getMessage());
        }
        catch (InvocationTargetException e) {
            throw new UnsupportedOperationException("Unsupported service " + serviceName + ": " + e.getMessage());
        }
    }

    public static Object getSingleton(String className) {
        try {
            Class<?> aClass = Class.forName(className);

            Method getInstance = aClass.getMethod("getInstance");

            return getInstance.invoke(null);
        }
        catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Unsupported singleton " + className + ": " + e.getMessage());
        }
        catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Unsupported singleton " + className + ": " + e.getMessage());
        }
        catch (IllegalAccessException e) {
            throw new UnsupportedOperationException("Unsupported singleton " + className + ": " + e.getMessage());
        }
        catch (InvocationTargetException e) {
            throw new UnsupportedOperationException("Unsupported singleton " + className + ": " + e.getMessage());
        }
    }
}
