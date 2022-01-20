package org.cloud.sonic.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Eason,stf
 * More https://github.com/DeviceFarmer/STFService.apk
 */
public class SearchActivity extends Activity {
    private static final String TAG = "SearchActivity";

    public static final String EXTRA_SERIAL = "serial";

    private interface SecuredGetter<T> {
        T get();
    }

    private String getSecuredId( SecuredGetter<String> supplier ) {
        try {
            return supplier.get();
        } catch(SecurityException e) {
            return "secured";
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        LinearLayout layout = new LinearLayout(this);
        layout.setKeepScreenOn(true);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        layout.setBackgroundColor(Color.parseColor("#409EFF"));
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(Gravity.CENTER);

        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        String serial = intent.getStringExtra(EXTRA_SERIAL);

        if (serial == null) {
            serial = getProperty("ro.serialno", "unknown");
        }

        layout.addView(createTitleLabel());
        layout.addView(createLabel("SERIAL"));
        layout.addView(createData(serial));
        layout.addView(createLabel("MODEL"));
        layout.addView(createData(getProperty("ro.product.model", "unknown")));
        layout.addView(createLabel("VERSION"));
        layout.addView(createData(Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"));
        layout.addView(createLabel("OPERATOR"));
        layout.addView(createData(tm.getSimOperatorName()));
        layout.addView(createLabel("PHONE"));
        layout.addView(createData(getSecuredId(tm::getLine1Number)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layout.addView(createLabel("IMEI"));
            layout.addView(createData(getSecuredId(tm::getImei)));
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        ensureVisibility();
        setContentView(layout);
    }

    private View createLabel(String text) {
        TextView titleView = new TextView(this);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(Color.parseColor("#000000"));
        titleView.setTextSize(16f);
        titleView.setText(text);
        return titleView;
    }

    private View createTitleLabel() {
        TextView titleView = new TextView(this);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(16,16,16,40);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(35f);
        titleView.setText("我在这里\nI am here");
        return titleView;
    }

    private View createData(String text) {
        TextView dataView = new TextView(this);
        dataView.setGravity(Gravity.CENTER);
        dataView.setTextColor(Color.WHITE);
        dataView.setTextSize(24f);
        dataView.setText(text);
        return dataView;
    }

    private void ensureVisibility() {
        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        unlock();

        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = 1.0f;
        window.setAttributes(params);
    }

    private String getProperty(String name, String defaultValue) {
        try {
            Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
            Method get = SystemProperties.getMethod("get", String.class, String.class);
            return (String) get.invoke(SystemProperties, name, defaultValue);
        }
        catch (ClassNotFoundException e) {
            Log.e(TAG, "Class.forName() failed", e);
            return defaultValue;
        }
        catch (NoSuchMethodException e) {
            Log.e(TAG, "getMethod() failed", e);
            return defaultValue;
        }
        catch (InvocationTargetException e) {
            Log.e(TAG, "invoke() failed", e);
            return defaultValue;
        }
        catch (IllegalAccessException e) {
            Log.e(TAG, "invoke() failed", e);
            return defaultValue;
        }
    }

    @SuppressWarnings("deprecation")
    private void unlock() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        keyguardManager.newKeyguardLock("InputService/Unlock").disableKeyguard();
    }

    public static class IntentBuilder {
        @Nullable private String serial;

        public IntentBuilder() {
        }

        public IntentBuilder serial(@NonNull String serial) {
            this.serial = serial;
            return this;
        }

        public Intent build(Context context) {
            Intent intent = new Intent(context.getApplicationContext(), SearchActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (serial != null) {
                intent.putExtra(SearchActivity.EXTRA_SERIAL, serial);
            }
            return intent;
        }
    }
}
