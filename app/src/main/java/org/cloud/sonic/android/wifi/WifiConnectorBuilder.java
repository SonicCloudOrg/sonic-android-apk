package org.cloud.sonic.android.wifi;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import org.cloud.sonic.android.wifi.wifi_connect.ConnectionScanResultsListener;
import org.cloud.sonic.android.wifi.wifi_connect.ConnectionSuccessListener;
import org.cloud.sonic.android.wifi.wifi_disconnect.DisconnectionStateListener;
import org.cloud.sonic.android.wifi.wifi_remove.RemoveStateListener;
import org.cloud.sonic.android.wifi.wifi_scan.ScanResultsListener;
import org.cloud.sonic.android.wifi.wifi_state.WifiStateListener;
import org.cloud.sonic.android.wifi.wifi_wps.ConnectionWpsListener;


public interface WifiConnectorBuilder {
    void start();

    interface WifiUtilsBuilder {
        void enableWifi(WifiStateListener wifiStateListener);

        void enableWifi();

        void disableWifi();

        @NonNull
        WifiConnectorBuilder scanWifi(@Nullable ScanResultsListener scanResultsListener);

        @NonNull
        WifiSuccessListener connectWith(@NonNull String ssid);

        @NonNull
        WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String password);

        @NonNull
        WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String bssid, @NonNull String password);

        WifiSuccessListener connectWith(@NonNull String ssid, @NonNull String password, @NonNull TypeEnum type);

        @NonNull
        WifiUtilsBuilder patternMatch();

        @Deprecated
        void disconnectFrom(@NonNull String ssid, @NonNull DisconnectionStateListener disconnectionSuccessListener);

        void disconnect(@NonNull DisconnectionStateListener disconnectionSuccessListener);

        void remove(@NonNull String ssid, @NonNull RemoveStateListener removeSuccessListener);

        @NonNull
        WifiSuccessListener connectWithScanResult(@NonNull String password, @Nullable ConnectionScanResultsListener connectionScanResultsListener);

        @NonNull
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        WifiWpsSuccessListener connectWithWps(@NonNull String bssid, @NonNull String password);

        void cancelAutoConnect();

        boolean isWifiConnected(@NonNull String ssid);
        boolean isWifiConnected();
    }

    interface WifiSuccessListener {
        @NonNull
        WifiSuccessListener setTimeout(long timeOutMillis);

        @NonNull
        WifiConnectorBuilder onConnectionResult(@Nullable ConnectionSuccessListener successListener);
    }

    interface WifiWpsSuccessListener {
        @NonNull
        WifiWpsSuccessListener setWpsTimeout(long timeOutMillis);

        @NonNull
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        WifiConnectorBuilder onConnectionWpsResult(@Nullable ConnectionWpsListener successListener);
    }
}
