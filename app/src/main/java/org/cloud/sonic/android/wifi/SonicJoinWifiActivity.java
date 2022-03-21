package org.cloud.sonic.android.wifi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ProxyInfo;

import android.os.Build;

import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.PatternsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import static org.cloud.sonic.android.wifi.JoinWifiConstant.*;
import static org.cloud.sonic.android.wifi.JoinWifiConstant.TAG;


import org.cloud.sonic.android.R;
import org.cloud.sonic.android.wifi.wifi_connect.ConnectionErrorCode;
import org.cloud.sonic.android.wifi.wifi_connect.ConnectionSuccessListener;

import java.text.ParseException;

public class SonicJoinWifiActivity extends AppCompatActivity {
    String mSSID;
    String mUsername;
    String mPassword;
    String mPasswordType;
    ProxyInfo mProxyInfo;

    String mIP;
    String mGateway;
    int mPrefix;
    String mDns1;
    String mDns2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(CLEAR_DEVICE_ADMIN)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            }
            else
            {
                throw new UnsupportedOperationException("API level 21 or higher required for this");
            }
            finish();
            return;
        }

        // Get Content
        mSSID = getIntent().getStringExtra(SSID);
        mPasswordType = getIntent().getStringExtra(PASSWORD_TYPE);
        mPassword = getIntent().getStringExtra(PASSWORD);
        mUsername = getIntent().getStringExtra(USERNAME);

        mIP = getIntent().getStringExtra(IP);
        mGateway = getIntent().getStringExtra(GATEWAY);
        mPrefix = getIntent().getIntExtra(PREFIX, 24);
        mDns1 = getIntent().getStringExtra(DNS1);
        mDns2 = getIntent().getStringExtra(DNS2);

        // Setup layout
        LinearLayout layout = new LinearLayout(this);
        setContentView(layout);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView textview = new TextView(this);
        textview.setText(getString(R.string.waiting));
        textview.setTextSize(20);
        layout.addView(textview, params);

        TextView SSIDtextview = new TextView(this);
        SSIDtextview.setText(mSSID);
        layout.addView(SSIDtextview, params);

        SonicWifiManager.withContext(this)
            .connectWith(mSSID, mPassword)
            .setTimeout(15000)
            .onConnectionResult(new ConnectionSuccessListener() {
                @Override
                public void success() {
                    switch (ContextCompat.checkSelfPermission(SonicJoinWifiActivity.this, Manifest.permission.WRITE_SECURE_SETTINGS)){
                        case PackageManager.PERMISSION_GRANTED:
                            String proxyHost = getIntent().getStringExtra(PROXY_HOST);
                            String proxyPort = getIntent().getStringExtra(PROXY_PORT);
                            if (proxyHost!=null && !proxyHost.equals("")){
                                String proxyInfo = ":0";
                                if (isValidIP(proxyHost) && isValidPort(proxyPort)){
                                    proxyInfo = proxyHost + ":" + proxyPort;
                                }else {
                                    Toast.makeText(SonicJoinWifiActivity.this, "Proxy Host or port set ERROR，please retry!", Toast.LENGTH_SHORT).show();
                                }
                                Settings.Global.putString(
                                    SonicJoinWifiActivity.this.getContentResolver(),
                                    Settings.Global.HTTP_PROXY,
                                    proxyInfo
                                );
                                Toast.makeText(SonicJoinWifiActivity.this, "SUCCESS!", Toast.LENGTH_SHORT).show();
                            }
                            finish();
                            break;
                        case PackageManager.PERMISSION_DENIED:
                        default:
                            Toast.makeText(SonicJoinWifiActivity.this, "Proxy setting ERROR，please try again!", Toast.LENGTH_SHORT).show();
                    }

                }

                @Override
                public void failed(ConnectionErrorCode errorCode) {
                    Toast.makeText(SonicJoinWifiActivity.this, "EPIC FAIL!$errorCode", Toast.LENGTH_SHORT).show();

                }
            }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public Boolean isValidIP(String input){
        return PatternsCompat.IP_ADDRESS.matcher(input).matches();
    }

    public Boolean isValidPort(String input){
        return !input.isEmpty() && TextUtils.isDigitsOnly(input) && 1 < Integer.parseInt(input) && Integer.parseInt(input)< 65535;
    }

}
