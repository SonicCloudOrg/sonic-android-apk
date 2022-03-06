package org.cloud.sonic.android.wifi.wifi_connect;


import static org.cloud.sonic.android.util.VersionUtils.isAndroidQOrLater;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.isAlreadyConnected;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.reEnableNetworkIfPossible;
import static org.cloud.sonic.android.wifi.SonicWifiManager.wifiLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Objects;


public final class WifiConnectionReceiver extends BroadcastReceiver {
    @NonNull
    private final WifiConnectionCallback mWifiConnectionCallback;
    @Nullable
    private ScanResult mScanResult;
    @NonNull
    private final WifiManager mWifiManager;
    private String ssid;


    public WifiConnectionReceiver(@NonNull final WifiConnectionCallback callback, @NonNull final WifiManager wifiManager) {
        this.mWifiConnectionCallback = callback;
        this.mWifiManager = wifiManager;
    }

    @Override
    public void onReceive(final Context context, @NonNull final Intent intent) {
        final String action = intent.getAction();
        wifiLog("Connection Broadcast action: " + action);
        if (isAndroidQOrLater()) {
            if (Objects.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, action)) {
                final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                final int suppl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                wifiLog("Connection Broadcast state: " + state);
                wifiLog("suppl_error: " + suppl_error);
                if (mScanResult == null && isAlreadyConnected2(mWifiManager, ssid)) {
                    mWifiConnectionCallback.connectSuccessful();
                }
                if (state == SupplicantState.DISCONNECTED && suppl_error == WifiManager.ERROR_AUTHENTICATING) {
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.ERROR_AUTHENTICATION);
                }
            }
        } else {
            if (Objects.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION, action)) {
                /*
                    Note here we don't check if has internet connectivity, because we only validate
                    if the connection to the hotspot is active, and not if the hotspot has internet.
                 */
                if (mScanResult!=null){
                    if (isAlreadyConnected(mWifiManager, mScanResult.BSSID)) {
                        mWifiConnectionCallback.connectSuccessful();
                    }
                }
            } else if (Objects.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, action)) {
                final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                final int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

                if (state == null) {
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.ERROR_CONNECT);
                    return;
                }

                wifiLog("Connection Broadcast state: " + state);

                switch (state) {
                    case COMPLETED:
                    case FOUR_WAY_HANDSHAKE:
                        if (mScanResult == null && isAlreadyConnected2(mWifiManager, ssid)) {
                            mWifiConnectionCallback.connectSuccessful();
                        } else if (isAlreadyConnected(mWifiManager, mScanResult.BSSID)) {
                            mWifiConnectionCallback.connectSuccessful();
                        }
                        break;
                    case DISCONNECTED:
                        if (supl_error == WifiManager.ERROR_AUTHENTICATING) {
                            wifiLog("Authentication error...");
                            mWifiConnectionCallback.errorConnect(ConnectionErrorCode.ERROR_AUTHENTICATION);
                        } else {
                            wifiLog("Disconnected. Re-attempting to connect...");
                            reEnableNetworkIfPossible(mWifiManager, mScanResult);
                        }
                }
            }
        }
    }

    public static boolean isAlreadyConnected2(@Nullable WifiManager wifiManager, @Nullable String ssid) {
        if (ssid != null && wifiManager != null) {
            if (wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getSSID() != null &&
                    wifiManager.getConnectionInfo().getIpAddress() != 0 &&
                    Objects.equals(ssid, wifiManager.getConnectionInfo().getSSID())) {
                wifiLog("Already connected to: " + wifiManager.getConnectionInfo().getSSID() + "  BSSID: " + wifiManager.getConnectionInfo().getBSSID());
                return true;
            }
        }
        return false;
    }

    @NonNull
    public WifiConnectionReceiver connectWith(@NonNull ScanResult result, @NonNull String password, @NonNull ConnectivityManager connectivityManager) {
        mScanResult = result;

        return this;
    }

    @NonNull
    public WifiConnectionReceiver connectWith(@NonNull String ssid, @NonNull String password, @NonNull ConnectivityManager connectivityManager) {
        this.ssid = ssid;

        return this;
    }
}
