package org.cloud.sonic.android;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;

public class SocketTest {
    @Test
    public void test() throws IOException, InterruptedException {
//        Runtime.getRuntime().exec("adb shell am start -n org.cloud.sonic.android/.SonicServiceActivity");
        Runtime.getRuntime().exec("adb shell ime enable org.cloud.sonic.android/.keyboard.SonicKeyboard");
        Runtime.getRuntime().exec("adb shell ime set org.cloud.sonic.android/.keyboard.SonicKeyboard");
        Thread.sleep(3000);
        Runtime.getRuntime().exec("adb forward tcp:2222 tcp:2335");
        Socket socket = new Socket("localhost",2222);
        System.out.println(socket.isConnected());
        socket.getOutputStream().write("测试".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(3000);
        socket.getOutputStream().write("王钊".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(3000);
        socket.getOutputStream().write("尝试删除".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(3000);
        socket.getOutputStream().write("CODE_AC_BACK".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(500);
        socket.getOutputStream().write("CODE_AC_BACK".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(500);
        socket.getOutputStream().write("CODE_AC_ENTER".getBytes());
        socket.getOutputStream().flush();
        Thread.sleep(3000);
        socket.getOutputStream().write("CODE_AC_CLEAN".getBytes());
        socket.getOutputStream().flush();
        socket.close();
        Runtime.getRuntime().exec("adb forward --remove tcp:2222");
    }
}
