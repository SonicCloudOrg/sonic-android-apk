package org.cloud.sonic.android;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;

public class SocketTest {
    @Test
    public void test() throws IOException, InterruptedException {
        Runtime.getRuntime().exec("adb shell am start -n org.cloud.sonic.android/.SonicServiceActivity");
        Thread.sleep(3000);
        Runtime.getRuntime().exec("adb forward tcp:7890 localabstract:sonicmanagersocket");
        Socket socket = new Socket("localhost",7890);
        System.out.println(socket.isConnected());
        Thread.sleep(3000);
        socket.close();
        Runtime.getRuntime().exec("adb forward --remove tcp:7890");
    }
}
