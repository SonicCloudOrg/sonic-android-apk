package org.cloud.sonic.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.cloud.sonic.android.models.AppInfo;
import org.cloud.sonic.android.util.ImgUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Eason
 * get App list
 */
public class AppListActivity extends Activity {
    private static final String TAG = "sonicapplistactivity";
    private static final String SOCKET = "sonicapplistservice";
    private LocalServerSocket serverSocket;

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
                tmpInfo.packageName = packageInfo.packageName;
                tmpInfo.versionName = packageInfo.versionName;
                tmpInfo.versionCode = packageInfo.versionCode;
                tmpInfo.appIcon = ImgUtil.drawableToDataUri(packageInfo.applicationInfo.loadIcon(getPackageManager()));
                try {
                    outputStream.write(JSON.toJSONString(tmpInfo).getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
