package org.cloud.sonic.android;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class SocketTest {
    @Test
    public void test() throws IOException, InterruptedException {
        Runtime.getRuntime().exec("adb shell am start -n org.cloud.sonic.android/.SonicServiceActivity");
//        Runtime.getRuntime().exec("adb shell ime enable org.cloud.sonic.android/.keyboard.SonicKeyboard");
//        Runtime.getRuntime().exec("adb shell ime set org.cloud.sonic.android/.keyboard.SonicKeyboard");
        Thread.sleep(3000);
        Runtime.getRuntime().exec("adb forward tcp:2222 tcp:2334");
        Socket socket = new Socket("localhost",2222);
        System.out.println(socket.isConnected());
        Thread.sleep(1000);
        socket.getOutputStream().write("action_get_all_app_info".getBytes());
//        socket.getOutputStream().write("action_get_all_wifi_info".getBytes());
        socket.getOutputStream().flush();
//        Thread.sleep(3000);
        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        System.out.println(br.readLine());
        Thread.sleep(10000);
        socket.close();
        Runtime.getRuntime().exec("adb forward --remove tcp:2222");

    }
}
