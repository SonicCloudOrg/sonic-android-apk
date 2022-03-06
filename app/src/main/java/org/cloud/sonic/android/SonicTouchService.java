/*
 *  Copyright (C) 2019 Orange
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
package org.cloud.sonic.android;

import android.graphics.Point;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.cloud.sonic.android.compat.InputManagerWrapper;
import org.cloud.sonic.android.compat.WindowManagerWrapper;
import org.cloud.sonic.android.util.InternalApi;

/**
 * @author Eason , stf
 * More https://github.com/DeviceFarmer/STFService.apk
 * Touch watcher
 */
@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SonicTouchService extends Thread {
    private static final String TAG = SonicTouchService.class.getSimpleName();
    private static final String SOCKET = "sonictouchservice";
    private static final int DEFAULT_MAX_CONTACTS = 10;
    private static final int DEFAULT_MAX_PRESSURE = 0;
    private static final int MAX_POINTERS = 2;
    private final int width;
    private final int height;
    private LocalServerSocket serverSocket;

    private MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[2];
    private MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[2];
    private PointerEvent[] events = new PointerEvent[2];

    private final InputManagerWrapper inputManager;
    private final WindowManagerWrapper windowManager;
    private final Handler handler;

    private class PointerEvent {
        long lastMouseDown;
        int lastX;
        int lastY;
        int action;
    }

    static Point getScreenSize() {
        Object displayManager = InternalApi.getSingleton("android.hardware.display.DisplayManagerGlobal");
        try {
            Object displayInfo = displayManager.getClass().getMethod("getDisplayInfo", int.class)
                .invoke(displayManager, Display.DEFAULT_DISPLAY);
            if (displayInfo != null) {
                Class<?> cls = displayInfo.getClass();
                int width = cls.getDeclaredField("logicalWidth").getInt(displayInfo);
                int height = cls.getDeclaredField("logicalHeight").getInt(displayInfo);
                return new Point(width, height);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        Looper.prepare();
        Handler handler = new Handler();
        Point size = getScreenSize();
        if (size != null) {
            SonicTouchService m = new SonicTouchService(size.x, size.y, handler);
            m.start();
            System.out.println("Server start");
            Looper.loop();
        } else {
            System.err.println("Couldn't get screen resolution");
            System.exit(1);
        }
    }

    private void injectEvent(InputEvent event) {
        handler.post(() -> inputManager.injectInputEvent(event));
    }

    private MotionEvent getMotionEvent(PointerEvent p) {
        return getMotionEvent(p, 0);
    }

    private MotionEvent getMotionEvent(PointerEvent p, int idx) {
        long now = SystemClock.uptimeMillis();
        if (p.action == MotionEvent.ACTION_DOWN) {
            p.lastMouseDown = now;
        }
        MotionEvent.PointerCoords coords = pointerCoords[idx];
        int rotation = windowManager.getRotation();
        double rad = Math.toRadians(rotation * 90.0);
        coords.x = (float) (p.lastX * Math.cos(-rad) - p.lastY * Math.sin(-rad));
        coords.y = (rotation * width) + (float) (p.lastX * Math.sin(-rad) + p.lastY * Math.cos(-rad));
        return MotionEvent.obtain(p.lastMouseDown, now, p.action, idx + 1, pointerProperties,
            pointerCoords, 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    private List<MotionEvent> getMotionEvent(PointerEvent p1, PointerEvent p2) {
        List<MotionEvent> combinedEvents = new ArrayList<>(2);
        long now = SystemClock.uptimeMillis();
        if (p1.action != MotionEvent.ACTION_MOVE) {
            combinedEvents.add(getMotionEvent(p1));
            combinedEvents.add(getMotionEvent(p2, 1));
        } else {
            MotionEvent.PointerCoords coords1 = pointerCoords[0];
            MotionEvent.PointerCoords coords2 = pointerCoords[1];
            int rotation = windowManager.getRotation();
            double rad = Math.toRadians(rotation * 90.0);

            coords1.x = (float) (p1.lastX * Math.cos(-rad) - p1.lastY * Math.sin(-rad));
            coords1.y = (rotation * width) + (float) (p1.lastX * Math.sin(-rad) + p1.lastY * Math.cos(-rad));

            coords2.x = (float) (p2.lastX * Math.cos(-rad) - p2.lastY * Math.sin(-rad));
            coords2.y = (rotation * width) + (float) (p2.lastX * Math.sin(-rad) + p2.lastY * Math.cos(-rad));

            MotionEvent event = MotionEvent.obtain(p1.lastMouseDown, now, p1.action, 2, pointerProperties,
                pointerCoords, 0, 0, 1f, 1f, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);
            combinedEvents.add(event);
        }
        return combinedEvents;
    }

    private void sendBanner(LocalSocket clientSocket) {
        try {
            OutputStreamWriter out = new OutputStreamWriter(clientSocket.getOutputStream());
            out.write("v 1\n");
            String resolution = String.format(Locale.US, "^ %d %d %d %d%n",
                DEFAULT_MAX_CONTACTS, width, height, DEFAULT_MAX_PRESSURE);
            out.write(resolution);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manageClientConnection() {
        while (true) {
            Log.i(TAG, String.format("Listening on %s", SOCKET));
            LocalSocket clientSocket;
            try {
                clientSocket = serverSocket.accept();
                Log.d(TAG, "client connected");
                sendBanner(clientSocket);
                processCommandLoop(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processCommandLoop(LocalSocket clientSocket) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String cmd;
            int count = 0;
            while ((cmd = in.readLine()) != null) {
                try (Scanner scanner = new Scanner(cmd)) {
                    scanner.useDelimiter(" ");
                    String type = scanner.next();
                    int contact;
                    switch (type) {
                        case "c":
                            if (count == 1) {
                                injectEvent(getMotionEvent(events[0]));
                            } else if (count == 2) {
                                for (MotionEvent event : getMotionEvent(events[0], events[1])) {
                                    injectEvent(event);
                                }
                            } else {
                                System.out.println("count not manage events #" + count);
                            }
                            count = 0;
                            break;
                        case "u":
                            count++;
                            contact = scanner.nextInt();
                            events[contact].action = (contact == 0) ? MotionEvent.ACTION_UP : MotionEvent.ACTION_POINTER_2_UP;
                            break;
                        case "d":
                            count++;
                            contact = scanner.nextInt();
                            events[contact].lastX = scanner.nextInt();
                            events[contact].lastY = scanner.nextInt();
                            //scanner.nextInt(); //pressure is currently not supported
                            events[contact].action = (contact == 0) ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_POINTER_2_DOWN;
                            break;
                        case "m":
                            count++;
                            contact = scanner.nextInt();
                            events[contact].lastX = scanner.nextInt();
                            events[contact].lastY = scanner.nextInt();
                            //scanner.nextInt(); //pressure is currently not supported
                            events[contact].action = MotionEvent.ACTION_MOVE;
                            break;
                        case "w":
                            int delayMs = scanner.nextInt();
                            Thread.sleep(delayMs);
                            break;
                        default:
                            System.out.println("could not parse: " + cmd);
                    }
                } catch (NoSuchElementException e) {
                    System.out.println("could not parse: " + cmd);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public SonicTouchService(int width, int height, Handler handler) {
        this.width = width;
        this.height = height;
        this.handler = handler;
        inputManager = new InputManagerWrapper();
        windowManager = new WindowManagerWrapper();
        initPointers();

//        MotionEvent.PointerProperties pointerProps0 = new MotionEvent.PointerProperties();
//        pointerProps0.id = 0;
//        pointerProps0.toolType = MotionEvent.TOOL_TYPE_FINGER;
//        MotionEvent.PointerProperties pointerProps1 = new MotionEvent.PointerProperties();
//        pointerProps1.id = 1;
//        pointerProps1.toolType = MotionEvent.TOOL_TYPE_FINGER;
//        pointerProperties[0] = pointerProps0;
//        pointerProperties[1] = pointerProps1;

//        MotionEvent.PointerCoords pointerCoords0 = new MotionEvent.PointerCoords();
//        MotionEvent.PointerCoords pointerCoords1 = new MotionEvent.PointerCoords();
//        pointerCoords0.orientation = 0;
//        pointerCoords0.pressure = 1;
//        pointerCoords0.size = 1;
//        pointerCoords1.orientation = 0;
//        pointerCoords1.pressure = 1;
//        pointerCoords1.size = 1;
//        pointerCoords[0] = pointerCoords0;
//        pointerCoords[1] = pointerCoords1;

        events[0] = new PointerEvent();
        events[1] = new PointerEvent();
    }

    public void initPointers(){
        for (int i=0; i < MAX_POINTERS; i++){
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 0;

            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
    }


    @Override
    public void run() {
        try {
            Log.i(TAG, String.format("creating socket %s", SOCKET));
            serverSocket = new LocalServerSocket(SOCKET);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        manageClientConnection();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
