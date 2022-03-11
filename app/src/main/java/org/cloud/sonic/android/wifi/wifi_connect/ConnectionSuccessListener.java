package org.cloud.sonic.android.wifi.wifi_connect;


import android.support.annotation.NonNull;

public interface ConnectionSuccessListener {
    void success();

    void failed(@NonNull ConnectionErrorCode errorCode);
}
