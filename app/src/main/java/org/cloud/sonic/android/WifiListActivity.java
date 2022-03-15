package org.cloud.sonic.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.cloud.sonic.android.models.SonicWifiInfo;
import org.cloud.sonic.android.models.WifiPacket;
import org.cloud.sonic.android.wifi.SonicWifiManager;
import org.cloud.sonic.android.wifi.wifi_scan.ScanResultsListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eason
 * get App list
 */
public class WifiListActivity extends Activity {
    private static final String TAG = "sonicwifilistactivity";
    private static final String SOCKET = "sonicawifilistservice";
    private LocalServerSocket serverSocket;

    /**
     * 数据缓冲大小，因为无法关闭Nagle，所以该参数没有意义
     */
    private static final int BUFFER_SIZE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "created");
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(() -> {
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
                Log.i(TAG, "client closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void manageClientConnection() {
        Log.i(TAG, String.format("Listening on %s", SOCKET));
        LocalSocket clientSocket;
        try {
            clientSocket = serverSocket.accept();
            //设置缓冲大小
            clientSocket.setReceiveBufferSize(BUFFER_SIZE);
            clientSocket.setSendBufferSize(BUFFER_SIZE);
            Log.d(TAG, "client connected");
            OutputStream outputStream = clientSocket.getOutputStream();
            getAllWifi(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("WrongConstant")
    public void getAllWifi(OutputStream outputStream) {
        List<PackageInfo> packages = getPackageManager().getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        SonicWifiManager.withContext(this)
            .scanWifi(new ScanResultsListener() {
                @Override
                public void onScanResults(@NonNull List<ScanResult> scanResults) {
                    WifiPacket wifiPacket = new WifiPacket();
                    wifiPacket.setConnectWifi(SonicWifiManager.withContext(WifiListActivity.this)
                        .isWifiConnected());
                    WifiInfo wifiInfo = SonicWifiManager.withContext(WifiListActivity.this)
                        .getWifiInfo();
                    wifiPacket.setConnectedWifi(SonicWifiInfo.transform(wifiInfo));
                    List<SonicWifiInfo> infos = new ArrayList<>();
                    for (ScanResult scresult:scanResults){
                        infos.add(SonicWifiInfo.transform(scresult));
                    }
                    wifiPacket.setWifiResults(infos);
                    try {
                        byte[] dataBytes = JSON.toJSONString(wifiPacket).getBytes();
                        // 数据长度转成二进制，存入byte[32]
                        byte[] lengthBytes = new byte[32];
                        String binStr = Integer.toBinaryString(dataBytes.length).trim();
                        char[] binArray = binStr.toCharArray();
                        for (int x = binArray.length-1, y = lengthBytes.length-1; x >= 0; x--, y--) {
                            try {
                                lengthBytes[y] = Byte.parseByte(binArray[x]+"");
                            } catch (Exception e) {
                                Log.e(TAG, String.format("char转byte失败，char为：【%s】", binArray[x] + ""));
                            }
                        }
                        // 先发送长度
                        outputStream.write(lengthBytes);
                        outputStream.flush();

                        // 再发送数据
                        outputStream.write(dataBytes);
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

    }

}
