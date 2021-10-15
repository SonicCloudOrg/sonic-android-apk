package com.sonic.plugins;

import static androidx.core.app.NotificationCompat.PRIORITY_MAX;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;


public class AutoInstallService extends AccessibilityService {
    private static final String TAG = "SonicAutoInstall";

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "onServiceConnected");
        super.onServiceConnected();
        String CHANNEL_ID = "com.sonic.plugins.notify";
        String CHANNEL_NAME = "Sonic安卓插件状态";
        NotificationChannel notificationChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).
                setContentTitle("已开启 Sonic安卓插件 服务").
                setContentText("服务正在运行，点击可查看配置。").
                setWhen(System.currentTimeMillis()).
                setSmallIcon(R.drawable.logo).
                setPriority(PRIORITY_MAX).
                setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)).
                setContentIntent(pendingIntent).build();
        startForeground(1, notification);
    }

    public void tapButton(List<AccessibilityNodeInfo> accessibilityNodeInfoList) {
        for (AccessibilityNodeInfo button : accessibilityNodeInfoList) {
            if (!button.getText().toString().contains("应用市场") && (!button.getText().toString().contains("软件商店"))) {
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                button.recycle();
            }
        }
    }

    private void sendPassword(AccessibilityNodeInfo rootNode, String password) {
        AccessibilityNodeInfo editText = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (editText == null) {
            try {
                Thread.sleep(1000);
                Runtime.getRuntime().exec("input text " + password);
                Runtime.getRuntime().exec("input keyevent 61");
                Runtime.getRuntime().exec("input keyevent 61");
                Runtime.getRuntime().exec("input keyevent 66");
            } catch (Exception e) {
                return;
            }
        } else {
            if ((editText.getPackageName().equals("com.bbk.account") || editText.getPackageName().equals("com.coloros.safecenter"))
                    && editText.getClassName().equals("android.widget.EditText")) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password);
                editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            }
            editText.recycle();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "onAccessibilityEvent: " + event.getPackageName());
        AccessibilityNodeInfo accessibilityNodeInfo = null;
        try {
            accessibilityNodeInfo = getRootInActiveWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (accessibilityNodeInfo == null) {
            return;
        }

        if (event.getPackageName().equals("android")) {
            List<AccessibilityNodeInfo> installButton = new ArrayList<>();
            try {
                installButton.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("安装"));
                installButton.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("允许"));
                tapButton(installButton);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (event.getPackageName().equals("com.vivo.secime.service") || event.getPackageName().equals("com.android.packageinstaller") || event.getPackageName().equals("com.coloros.safecenter")) {
            try {
                String password = (String) DataFormatUtils.getParam(getApplicationContext(),
                        OptionData.KEY_PASSWORD, "sonic123456");
                if (!TextUtils.isEmpty(password)) {
                    sendPassword(accessibilityNodeInfo, password);
                }
                List<AccessibilityNodeInfo> nodeInfoList = new ArrayList<>();
                nodeInfoList.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("确定"));
                nodeInfoList.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("继续安装"));
                nodeInfoList.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("安装"));
                nodeInfoList.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("完成"));
                nodeInfoList.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("允许"));
                nodeInfoList.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("始终允许"));
                tapButton(nodeInfoList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (event.getPackageName().equals("com.android.permissioncontroller")) {
            try {
                List<AccessibilityNodeInfo> nodeInfoList = new ArrayList<>();
                nodeInfoList.addAll(accessibilityNodeInfo.findAccessibilityNodeInfosByText("允许"));
                tapButton(nodeInfoList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        accessibilityNodeInfo.recycle();
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "onInterrupt");
    }


}
