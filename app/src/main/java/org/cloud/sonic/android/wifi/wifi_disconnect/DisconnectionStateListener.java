package org.cloud.sonic.android.wifi.wifi_disconnect;


import android.support.annotation.NonNull;

public interface DisconnectionStateListener {
    void success();

    void failed(@NonNull DisconnectionErrorCode errorCode);
}
