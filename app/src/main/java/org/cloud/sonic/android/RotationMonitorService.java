package org.cloud.sonic.android;

import org.cloud.sonic.android.compat.WindowManagerWrapper;

/**
 * @author Eason,stf
 * More https://github.com/DeviceFarmer/STFService.apk
 * Rotation Watcher
 */
public class RotationMonitorService extends Thread {
    private WindowManagerWrapper wmw = new WindowManagerWrapper();

    public static void main(String[] args) {
        try {
            RotationMonitorService monitor = new RotationMonitorService();
            monitor.start();
            monitor.join();
        }
        catch (InterruptedException e) {

        }
    }

    @Override
    public void run() {
        WindowManagerWrapper.RotationWatcher watcher = rotation -> System.out.println(rotation);
        try {
            System.out.println(wmw.getRotation());
            wmw.watchRotation(watcher);

            synchronized (this) {
                while (!isInterrupted()) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
        }
    }
}
