package org.cloud.sonic.android.wifi.wifi_connect;

import static org.cloud.sonic.android.util.VersionUtils.isAndroidQOrLater;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.isAlreadyConnected;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.reEnableNetworkIfPossible;
import static org.cloud.sonic.android.wifi.SonicWifiManager.wifiLog;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import org.cloud.sonic.android.wifi.WeakHandler;

public class TimeoutHandler {
    private final WifiManager mWifiManager;
    private final WeakHandler mHandler;
    private final WifiConnectionCallback mWifiConnectionCallback;
    private ScanResult mScanResult;

    private final Runnable timeoutCallback = new Runnable() {
        @Override
        public void run() {
            wifiLog("Connection Timed out...");

            if (!isAndroidQOrLater()) {
                reEnableNetworkIfPossible(mWifiManager, mScanResult);
            }
            if (mScanResult.BSSID!=null){
                if (isAlreadyConnected(mWifiManager, mScanResult.BSSID)) {
                    mWifiConnectionCallback.connectSuccessful();
                } else {
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.ERROR_TIMEOUT);
                }
            }
            mHandler.removeCallbacks(this);
        }
    };

    public TimeoutHandler(@NonNull WifiManager wifiManager, @NonNull WeakHandler handler, @NonNull final WifiConnectionCallback wifiConnectionCallback) {
        this.mWifiManager = wifiManager;
        this.mHandler = handler;
        this.mWifiConnectionCallback = wifiConnectionCallback;
    }

    public void startTimeout(final ScanResult scanResult, final long timeout) {
        // cleanup previous connection timeout handler
        mHandler.removeCallbacks(timeoutCallback);

        mScanResult = scanResult;
        mHandler.postDelayed(timeoutCallback, timeout);
    }

    public void stopTimeout() {
        mHandler.removeCallbacks(timeoutCallback);
    }
}
