package org.cloud.sonic.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.cloud.sonic.android.models.AppInfo;
import org.cloud.sonic.android.util.ImgUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Eason
 * get App list
 */
public class AppListActivity extends Activity {
    private static final String TAG = "sonicapplistactivity";
    private static final String SOCKET = "sonicapplistservice";
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
            getAllApp(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("WrongConstant")
    public void getAllApp(OutputStream outputStream) {
        List<PackageInfo> packages = getPackageManager().getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                AppInfo tmpInfo = new AppInfo();
                tmpInfo.appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    tmpInfo.packageName = new String(packageInfo.packageName.getBytes(StandardCharsets.UTF_8));
                } else {
                    tmpInfo.packageName = packageInfo.packageName;
                }
                tmpInfo.versionName = packageInfo.versionName;
                tmpInfo.versionCode = packageInfo.versionCode;
                tmpInfo.appIcon = ImgUtil.drawableToDataUri(packageInfo.applicationInfo.loadIcon(getPackageManager()));
                try {
                    byte[] dataBytes = JSON.toJSONString(tmpInfo).getBytes();
                    // 数据长度转成二进制，存入byte[32]
                    byte[] lengthBytes = new byte[32];
                    String binStr = Integer.toBinaryString(dataBytes.length);
                    String[] binArray = binStr.split("");
                    for (int x = binArray.length-1, y = lengthBytes.length-1; x >= 0; x--, y--) {
                        lengthBytes[y] = Byte.parseByte(binArray[x]);
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
        }
    }

}
