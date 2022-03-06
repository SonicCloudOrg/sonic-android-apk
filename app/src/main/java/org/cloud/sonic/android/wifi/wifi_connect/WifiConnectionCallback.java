package org.cloud.sonic.android.wifi.wifi_connect;


import android.support.annotation.NonNull;

public interface WifiConnectionCallback {
    void connectSuccessful();
    void errorConnect(@NonNull ConnectionErrorCode connectionErrorCode);
}
