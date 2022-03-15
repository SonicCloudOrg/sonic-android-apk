package org.cloud.sonic.android;

import android.app.Activity;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.cloud.sonic.android.wifi.SonicWifiManager;
import org.cloud.sonic.android.wifi.wifi_scan.ScanResultsListener;

import java.util.List;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
