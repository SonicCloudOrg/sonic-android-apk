package org.cloud.sonic.android;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;

public class SocketTest {
    @Test
    public void test() throws IOException, InterruptedException {
        Runtime.getRuntime().exec("adb shell am start -n org.cloud.sonic.android/.SonicServiceActivity");
        Thread.sleep(3000);
        Runtime.getRuntime().exec("adb forward tcp:2222 tcp:2334");
        Socket socket = new Socket("localhost",2222);
        System.out.println(socket.isConnected());
        socket.getOutputStream().write("action_get_all_app_info".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(3000);
        socket.getOutputStream().write("action_get_all_wifi_info".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(3000);
        socket.getOutputStream().write("org.cloud.sonic.android.STOP".getBytes());
        socket.getOutputStream().flush();
        socket.close();
        Runtime.getRuntime().exec("adb forward --remove tcp:2222");
    }
}
