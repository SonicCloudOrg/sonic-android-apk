package org.cloud.sonic.android.wifi.wifi_remove;


import android.support.annotation.NonNull;

public interface RemoveStateListener {
    void removeSuccess();
    void removeFailed(@NonNull RemoveErrorCode errorCode);
}
