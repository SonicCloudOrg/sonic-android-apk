package org.cloud.sonic.android;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketTest {
    @Test
    public void test() throws IOException, InterruptedException {
        Runtime.getRuntime().exec("adb shell am start -n org.cloud.sonic.android/.SonicServiceActivity");
        Thread.sleep(3000);
        Runtime.getRuntime().exec("adb forward tcp:2222 tcp:2334");
        Thread.sleep(1000);
        Socket socket = new Socket("localhost",2222);
        System.out.println(socket.isConnected());
        Thread.sleep(1000);
//        socket.getOutputStream().write("action_get_all_app_info".getBytes());
        socket.getOutputStream().write("action_get_all_wifi_info".getBytes());
        socket.getOutputStream().flush();
        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String s;
        while (true){
            try {
                if ((s = br.readLine()) == null) break;
            }catch (IOException e){
                e.printStackTrace();
                break;
            }
            System.out.println(s);
        }
        socket.close();
        Runtime.getRuntime().exec("adb forward --remove tcp:2222");

    }

    @Test
    public void test2() throws IOException, InterruptedException {
        Runtime.getRuntime().exec("adb shell ime enable org.cloud.sonic.android/.keyboard.SonicKeyboard");
        Runtime.getRuntime().exec("adb shell ime set org.cloud.sonic.android/.keyboard.SonicKeyboard");
        Thread.sleep(3000);
        Runtime.getRuntime().exec("adb forward tcp:2222 tcp:2335");
        Thread.sleep(1000);
        Socket socket = new Socket("localhost",2222);
        System.out.println(socket.isConnected());
        Thread.sleep(1000);
        socket.getOutputStream().write("Hello我是中文".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        Thread.sleep(1000);
        socket.getOutputStream().write("CODE_AC_BACK".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        Thread.sleep(1000);
        socket.getOutputStream().write("CODE_AC_ENTER".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        Thread.sleep(1000);
        socket.getOutputStream().write("CODE_AC_CLEAN".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        Thread.sleep(3000);
        socket.close();
        Runtime.getRuntime().exec("adb forward --remove tcp:2222");
    }
}
