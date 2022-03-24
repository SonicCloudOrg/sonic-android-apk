package org.cloud.sonic.android;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.text.method.MetaKeyKeyListener;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImeService extends InputMethodService {
    private static final String ASCII = "US-ASCII";
    private static final int NOTIFICATION_ID = 2;
    private static final String TAG = "SonicImeService";
    private static final String UTF7 = "UTF-7";
    private static final char UTF7_SHIFT = '+';
    private static final char UTF7_UNSHIFT = '-';
    public static Boolean ioThreadFlag = Boolean.TRUE;
    public static Boolean mainThreadFlag= Boolean.TRUE;
    /* access modifiers changed from: private */
    public String ADB_IME_MESSAGE = "ADB_INPUT_TEXT";
    /* access modifiers changed from: private */
    public String IME_CHARS = "ADB_INPUT_CHARS";
    /* access modifiers changed from: private */
    public String IME_EDITORCODE = "ADB_EDITOR_CODE";
    /* access modifiers changed from: private */
    public String IME_KEYCODE = "ADB_INPUT_CODE";
    /* access modifiers changed from: private */
    public String IME_MESSAGE = "INPUT_TEXT";
    final int SERVER_PORT = 10086;
    private String USB_STATE_CHANGE = "android.hardware.usb.action.USB_STATE";
    private StringBuilder mComposing;
    private boolean mIsShifted;
    private long mMetaState;
    private BroadcastReceiver mReceiver = null;
    private Charset mUtf7Charset;
    ServerSocket serverSocket = null;

    public class AdbReceiver extends BroadcastReceiver {
        public AdbReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean equals = intent.getAction().equals(ImeService.this.IME_MESSAGE);
            String str = ImeService.TAG;
            if (equals) {
                String msg = intent.getStringExtra("text");
                if (msg != null) {
                    InputConnection ic = ImeService.this.getCurrentInputConnection();
                    if (ic != null) {
                        try {
                            String msgEncode = ImeService.unicodeToString(msg);
                            Log.i(str, "Ime received: " + msg + "convert to unicode is: " + msgEncode);
                            ic.commitText(msgEncode, 1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    return;
                }
            }
            if (intent.getAction().equals(ImeService.this.ADB_IME_MESSAGE)) {
                String msg = intent.getStringExtra("msg");
                if (msg != null) {
                    String format = intent.getStringExtra("format");
                    Log.d(TAG, "Input Format: " + format);
                    if (format != null && format.equals("base64")) {
                        msg = ImeService.this.decodeUtf7(new String(Base64.decode(msg, Base64.DEFAULT)));
                    }
                    InputConnection ic2 = ImeService.this.getCurrentInputConnection();
                    if (ic2 != null) {
                        Log.d(str, "Input message: " + msg);
                        ic2.commitText(msg, 1);
                    }
                }
            }
            if (intent.getAction().equals(ImeService.this.IME_CHARS)) {
                int[] chars = intent.getIntArrayExtra("chars");
                if (chars != null) {
                    String msg = new String(chars, 0, chars.length);
                    InputConnection ic3 = ImeService.this.getCurrentInputConnection();
                    if (ic3 != null) {
                        ic3.commitText(msg, 1);
                    }
                }
            }
            String str3 = "code";
            if (intent.getAction().equals(ImeService.this.IME_KEYCODE)) {
                int code = intent.getIntExtra(str3, -1);
                int repeat = intent.getIntExtra("repeat", 1);
                if (code != -1) {
                    InputConnection ic4 = ImeService.this.getCurrentInputConnection();
                    if (ic4 != null) {
                        for (int i = 0; i < repeat; i++) {
                            ic4.sendKeyEvent(new KeyEvent(0, code));
                        }
                    }
                }
            }
            if (intent.getAction().equals(ImeService.this.IME_EDITORCODE)) {
                int code2 = intent.getIntExtra(str3, -1);
                if (code2 != -1) {
                    InputConnection ic5 = ImeService.this.getCurrentInputConnection();
                    if (ic5 != null) {
                        ic5.performEditorAction(code2);
                    }
                }
            }
        }
    }


    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView()");
        new Bundle();
        View mInputView = getLayoutInflater().inflate(R.layout.keyboard, null);
        mInputView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent("android.settings.INPUT_METHOD_SETTINGS");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    ImeService.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(ImeService.this, "Launch app failed!", Toast.LENGTH_LONG).show();
                }
            }
        });
        if (this.mReceiver == null) {
            IntentFilter filter = new IntentFilter(this.IME_MESSAGE);
            filter.addAction(this.ADB_IME_MESSAGE);
            filter.addAction(this.IME_CHARS);
            filter.addAction(this.IME_KEYCODE);
            filter.addAction(this.IME_EDITORCODE);
            filter.addAction(USB_STATE_CHANGE);
            this.mReceiver = new AdbReceiver();
            registerReceiver(this.mReceiver, filter);
        }
        return mInputView;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onInitializeInterface() {
        showNotifications();
    }

    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (!restarting) {
            this.mMetaState = 0;
            this.mIsShifted = false;
            this.mUtf7Charset = Charset.forName(UTF7);
        }
        this.mComposing = null;
    }

    public void onFinishInput() {
        super.onFinishInput();
        this.mUtf7Charset = null;
        this.mComposing = null;
    }

    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        this.mMetaState = MetaKeyKeyListener.handleKeyUp(this.mMetaState, keyCode, event);
        return super.onKeyUp(keyCode, event);
    }

    /* access modifiers changed from: private */
    public String decodeUtf7(String encStr) {
        return new String(encStr.getBytes(Charset.forName(ASCII)), this.mUtf7Charset);
    }

    public void onDestroy() {
        BroadcastReceiver broadcastReceiver = this.mReceiver;
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        cancelNotifications();
        mainThreadFlag = false;
        ioThreadFlag = false;
        Log.v(TAG, "---->serverSocket.close()");
        try {

            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v(TAG, Thread.currentThread().getName() + "---->**************** onDestroy****************");
        super.onDestroy();
    }

    public static String unicodeToString(String msg) {
        String str = msg.replace("\\\\", "\\");
        Matcher matcher = Pattern.compile("(\\\\u(\\p{XDigit}{4}))").matcher(str);
        while (matcher.find()) {
            char ch = (char) Integer.parseInt(matcher.group(2), 16);
            String group = matcher.group(1);
            StringBuilder sb = new StringBuilder();
            sb.append(ch);
            sb.append("");
            str = str.replace(group, sb.toString());
        }
        return str;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            NotificationChannel channel = new NotificationChannel(TAG, getString(R.string.app_name), NotificationManager.IMPORTANCE_NONE);
            getNotificationManager().createNotificationChannel(channel);
        }

        startForeground(NOTIFICATION_ID,
            new NotificationCompat.Builder(this, TAG)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(getString(R.string.ime_service_name))
                .setContentTitle(getString(R.string.ime_service_title))
                .setContentText(getString(R.string.ime_service_content))
                .setWhen(System.currentTimeMillis()).build());
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void cancelNotifications() {
        getNotificationManager().cancel(NOTIFICATION_ID);
    }
}
