package org.cloud.sonic.android.wifi;


import static org.cloud.sonic.android.util.Elvis.of;
import static org.cloud.sonic.android.util.VersionUtils.isAndroidQOrLater;
import static org.cloud.sonic.android.util.VersionUtils.isLollipopOrLater;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.cleanPreviousConfiguration;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.connectToWifi;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.connectToWifiHidden;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.connectWps;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.disconnectFromWifi;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.isAlreadyConnected;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.matchScanResult;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.matchScanResultBssid;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.matchScanResultSsid;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.reenableAllHotspots;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.registerReceiver;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.removeWifi;
import static org.cloud.sonic.android.wifi.WifiConnectorUtils.unregisterReceiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.cloud.sonic.android.recorder.utils.Logger;
import org.cloud.sonic.android.wifi.wifi_connect.ConnectionErrorCode;
import org.cloud.sonic.android.wifi.wifi_connect.ConnectionScanResultsListener;
import org.cloud.sonic.android.wifi.wifi_connect.ConnectionSuccessListener;
import org.cloud.sonic.android.wifi.wifi_connect.DisconnectCallbackHolder;
import org.cloud.sonic.android.wifi.wifi_connect.TimeoutHandler;
import org.cloud.sonic.android.wifi.wifi_connect.WifiConnectionCallback;
import org.cloud.sonic.android.wifi.wifi_connect.WifiConnectionReceiver;
import org.cloud.sonic.android.wifi.wifi_disconnect.DisconnectionErrorCode;
import org.cloud.sonic.android.wifi.wifi_disconnect.DisconnectionStateListener;
import org.cloud.sonic.android.wifi.wifi_remove.RemoveErrorCode;
import org.cloud.sonic.android.wifi.wifi_remove.RemoveStateListener;
import org.cloud.sonic.android.wifi.wifi_scan.ScanResultsListener;
import org.cloud.sonic.android.wifi.wifi_scan.WifiScanCallback;
import org.cloud.sonic.android.wifi.wifi_scan.WifiScanReceiver;
import org.cloud.sonic.android.wifi.wifi_state.WifiStateCallback;
import org.cloud.sonic.android.wifi.wifi_state.WifiStateListener;
import org.cloud.sonic.android.wifi.wifi_state.WifiStateReceiver;
import org.cloud.sonic.android.wifi.wifi_wps.ConnectionWpsListener;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class SonicWifiManager implements WifiConnectorBuilder,
    WifiConnectorBuilder.WifiUtilsBuilder,
    WifiConnectorBuilder.WifiSuccessListener,
    WifiConnectorBuilder.WifiWpsSuccessListener {

    private long mWpsTimeoutMillis = 30000;
    private long mTimeoutMillis = 30000;
    @Nullable
    private final WifiManager mWifiManager;
    @NonNull
    private Context mContext;
    @Nullable
    private final ConnectivityManager mConnectivityManager;
    private static boolean mEnableLog=true;
    @Nullable
    private static Logger customLogger;
    @NonNull
    private WeakHandler mHandler;
    @NonNull
    private final WifiStateReceiver mWifiStateReceiver;
    @NonNull
    private final WifiConnectionReceiver mWifiConnectionReceiver;
    @NonNull
    private final TimeoutHandler mTimeoutHandler;
    @NonNull
    private final WifiScanReceiver mWifiScanReceiver;
    @Nullable
    private String mSsid;
    @Nullable
    private String type;
    @Nullable
    private String mBssid;
    @Nullable
    private String mPassword;
    @Nullable
    private ScanResult mSingleScanResult;
    @Nullable
    private ScanResultsListener mScanResultsListener;
    @Nullable
    private ConnectionScanResultsListener mConnectionScanResultsListener;
    @Nullable
    private ConnectionSuccessListener mConnectionSuccessListener;
    @Nullable
    private WifiStateListener mWifiStateListener;
    @Nullable
    private ConnectionWpsListener mConnectionWpsListener;
    @Nullable
    private boolean mPatternMatch;

    @NonNull
    private final WifiStateCallback mWifiStateCallback = new WifiStateCallback() {
        @Override
        public void onWifiEnabled() {
            wifiLog("WIFI ENABLED...");
            unregisterReceiver(mContext, mWifiStateReceiver);
            of(mWifiStateListener).ifPresent(stateListener -> stateListener.isSuccess(true));

            if (mScanResultsListener != null || mPassword != null) {
                wifiLog("START SCANNING....");
                if (mWifiManager.startScan()) {
                    registerReceiver(mContext, mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                } else {
                    of(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(new ArrayList<>()));
                    of(mConnectionWpsListener).ifPresent(wpsListener -> wpsListener.isSuccessful(false));
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.ERROR_SCAN);
                    wifiLog("ERROR COULDN'T SCAN");
                }
            }
        }
    };

    @NonNull
    private final WifiScanCallback mWifiScanResultsCallback = new WifiScanCallback() {
        @Override
        public void onScanResultsReady() {
            wifiLog("GOT SCAN RESULTS");
            unregisterReceiver(mContext, mWifiScanReceiver);

            final List<ScanResult> scanResultList = mWifiManager.getScanResults();

            of(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(scanResultList));
            of(mConnectionScanResultsListener).ifPresent(connectionResultsListener -> mSingleScanResult = connectionResultsListener.onConnectWithScanResult(scanResultList));

            if (mConnectionWpsListener != null && mBssid != null && mPassword != null) {
                mSingleScanResult = matchScanResultBssid(mBssid, scanResultList);
                if (mSingleScanResult != null && isLollipopOrLater()) {
                    connectWps(mWifiManager, mHandler, mSingleScanResult, mPassword, mWpsTimeoutMillis, mConnectionWpsListener);
                } else {
                    if (mSingleScanResult == null) {
                        wifiLog("Couldn't find network. Possibly out of range");
                    }
                    mConnectionWpsListener.isSuccessful(false);
                }
                return;
            }

            if (mSsid != null) {
                if (mBssid != null) {
                    mSingleScanResult = matchScanResult(mSsid, mBssid, scanResultList);
                } else {
                    mSingleScanResult = matchScanResultSsid(mSsid, scanResultList, mPatternMatch);
                }
            }
            if (mSingleScanResult != null && mPassword != null) {
                if (connectToWifi(mContext, mWifiManager, mConnectivityManager, mHandler, mSingleScanResult, mPassword, mWifiConnectionCallback, mPatternMatch, mSsid)) {
                    registerReceiver(mContext, (mWifiConnectionReceiver).connectWith(mSingleScanResult, mPassword, mConnectivityManager),
                        new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                    registerReceiver(mContext, mWifiConnectionReceiver,
                        new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
                    mTimeoutHandler.startTimeout(mSingleScanResult, mTimeoutMillis);
                } else {
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.ERROR_CONNECT);
                }
            } else {
                if (connectToWifiHidden(mContext, mWifiManager, mConnectivityManager, mHandler, mSsid, type, mPassword, mWifiConnectionCallback)) {
                    registerReceiver(mContext, (mWifiConnectionReceiver).connectWith(mSsid, mPassword, mConnectivityManager),
                        new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
                    registerReceiver(mContext, mWifiConnectionReceiver,
                        new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
                    mTimeoutHandler.startTimeout(mSingleScanResult, mTimeoutMillis);
                } else {
                    mWifiConnectionCallback.errorConnect(ConnectionErrorCode.ERROR_CONNECT);
                }
            }
        }
    };

    @NonNull
    private final WifiConnectionCallback mWifiConnectionCallback = new WifiConnectionCallback() {

        @Override
        public void connectSuccessful() {
            wifiLog("CONNECTED SUCCESSFULLY");
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            mTimeoutHandler.stopTimeout();

            //reenableAllHotspots(mWifiManager);
            of(mConnectionSuccessListener).ifPresent(ConnectionSuccessListener::success);
        }

        @Override
        public void errorConnect(@NonNull ConnectionErrorCode connectionErrorCode) {
            unregisterReceiver(mContext, mWifiConnectionReceiver);
            mTimeoutHandler.stopTimeout();
            if (isAndroidQOrLater()) {
                DisconnectCallbackHolder.getInstance().disconnect();
            }
            reenableAllHotspots(mWifiManager);
            //if (mSingleScanResult != null)
            //cleanPreviousConfiguration(mWifiManager, mSingleScanResult);
            of(mConnectionSuccessListener).ifPresent(successListener -> {
                successListener.failed(connectionErrorCode);
                wifiLog("DIDN'T CONNECT TO WIFI " + connectionErrorCode);
            });
        }
    };

    public SonicWifiManager(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null) {
            throw new RuntimeException("WifiManager is not null, Why are you here?");
        }
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiStateReceiver = new WifiStateReceiver(mWifiStateCallback);
        mWifiScanReceiver = new WifiScanReceiver(mWifiScanResultsCallback);
        mHandler = new WeakHandler();
        mWifiConnectionReceiver = new WifiConnectionReceiver(mWifiConnectionCallback, mWifiManager);
        mTimeoutHandler = new TimeoutHandler(mWifiManager, mHandler, mWifiConnectionCallback);
    }

    public static WifiUtilsBuilder withContext(@NonNull final Context context) {
        return new SonicWifiManager(context);
    }

    public static void wifiLog(final String text) {
        if (mEnableLog) {

        }
    }

    @Override
    public void enableWifi(@Nullable final WifiStateListener wifiStateListener) {
        mWifiStateListener = wifiStateListener;
        if (mWifiManager.isWifiEnabled()) {
            mWifiStateCallback.onWifiEnabled();
        } else {
            if (mWifiManager.setWifiEnabled(true)) {
                registerReceiver(mContext, mWifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            } else {
                if (wifiStateListener !=null){

                }
                of(wifiStateListener).ifPresent(stateListener -> stateListener.isSuccess(false));
                of(mScanResultsListener).ifPresent(resultsListener -> resultsListener.onScanResults(new ArrayList<>()));
                of(mConnectionWpsListener).ifPresent(wpsListener -> wpsListener.isSuccessful(false));
                mWifiConnectionCallback.errorConnect(ConnectionErrorCode.ERROR_ENABLE_WIFI);
                wifiLog("COULDN'T ENABLE WIFI");
            }
        }
    }

    @Override
    public void enableWifi() {
        enableWifi(null);
    }

    @NonNull
    @Override
    public WifiConnectorBuilder scanWifi(final ScanResultsListener scanResultsListener) {
        mScanResultsListener = scanResultsListener;
        return this;
    }

    @Deprecated
    @Override
    public void disconnectFrom(@NonNull final String ssid, @NonNull final DisconnectionStateListener disconnectionSuccessListener) {
        this.disconnect(disconnectionSuccessListener);
    }

    @Override
    public void disconnect(@NonNull DisconnectionStateListener disconnectionSuccessListener) {
        if (mConnectivityManager == null) {
            disconnectionSuccessListener.failed(DisconnectionErrorCode.ERROR_GET_CONNECTIVITY_MANAGER);
            return;
        }

        if (mWifiManager == null) {
            disconnectionSuccessListener.failed(DisconnectionErrorCode.ERROR_GET_WIFI_MANAGER);
            return;
        }

        if (isAndroidQOrLater()) {
            DisconnectCallbackHolder.getInstance().unbindProcessFromNetwork();
            DisconnectCallbackHolder.getInstance().disconnect();
            disconnectionSuccessListener.success();
        } else {
            if (disconnectFromWifi(mWifiManager)) {
                disconnectionSuccessListener.success();
            } else {
                disconnectionSuccessListener.failed(DisconnectionErrorCode.ERROR_DISCONNECT);
            }
        }
    }


    @Override
    public void remove(@NonNull String ssid, @NonNull RemoveStateListener removeSuccessListener) {
        if (mConnectivityManager == null) {
            removeSuccessListener.removeFailed(RemoveErrorCode.ERROR_GET_CONNECTIVITY_MANAGER);
            return;
        }

        if (mWifiManager == null) {
            removeSuccessListener.removeFailed(RemoveErrorCode.ERROR_GET_WIFI_MANAGER);
            return;
        }

        if (isAndroidQOrLater()) {
            DisconnectCallbackHolder.getInstance().unbindProcessFromNetwork();
            DisconnectCallbackHolder.getInstance().disconnect();
            removeSuccessListener.removeSuccess();
        } else {
            if (removeWifi(mWifiManager, ssid)) {
                removeSuccessListener.removeSuccess();
            } else {
                removeSuccessListener.removeFailed(RemoveErrorCode.ERROR_REMOVE);
            }
        }
    }

    @NonNull
    @Override
    public WifiUtilsBuilder patternMatch() {
        mPatternMatch = true;

        return this;
    }


    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid) {
        mSsid = ssid;
        mPassword = ""; // FIXME: Cover no password case

        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String password) {
        mSsid = ssid;
        mPassword = password;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String password, @NonNull final TypeEnum type) {
        mSsid = ssid;
        mPassword = password;
        this.type = type.name();
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWith(@NonNull final String ssid, @NonNull final String bssid, @NonNull final String password) {
        mSsid = ssid;
        mBssid = bssid;
        mPassword = password;
        return this;
    }

    @NonNull
    @Override
    public WifiSuccessListener connectWithScanResult(@NonNull final String password,
                                                     @Nullable final ConnectionScanResultsListener connectionScanResultsListener) {
        mConnectionScanResultsListener = connectionScanResultsListener;
        mPassword = password;
        return this;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiWpsSuccessListener connectWithWps(@NonNull final String bssid, @NonNull final String password) {
        mBssid = bssid;
        mPassword = password;
        return this;
    }


    @Override
    public void cancelAutoConnect() {
        unregisterReceiver(mContext, mWifiStateReceiver);
        unregisterReceiver(mContext, mWifiScanReceiver);
        unregisterReceiver(mContext, mWifiConnectionReceiver);
        of(mSingleScanResult).ifPresent(scanResult -> cleanPreviousConfiguration(mWifiManager, scanResult));
        reenableAllHotspots(mWifiManager);
    }

    @Override
    public boolean isWifiConnected(@NonNull String ssid) {
        return isAlreadyConnected(mWifiManager, mConnectivityManager, ssid);
    }

    @Override
    public boolean isWifiConnected() {
        return isAlreadyConnected(mConnectivityManager);
    }

    @NonNull
    @Override
    public WifiSuccessListener setTimeout(final long timeOutMillis) {
        mTimeoutMillis = timeOutMillis;
        return this;
    }

    @NonNull
    @Override
    public WifiWpsSuccessListener setWpsTimeout(final long timeOutMillis) {
        mWpsTimeoutMillis = timeOutMillis;
        return this;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WifiConnectorBuilder onConnectionWpsResult(@Nullable final ConnectionWpsListener successListener) {
        mConnectionWpsListener = successListener;
        return this;
    }


    @NonNull
    @Override
    public WifiConnectorBuilder onConnectionResult(@Nullable final ConnectionSuccessListener successListener) {
        mConnectionSuccessListener = successListener;
        return this;
    }

    @Override
    public void start() {
        unregisterReceiver(mContext, mWifiStateReceiver);
        unregisterReceiver(mContext, mWifiScanReceiver);
        unregisterReceiver(mContext, mWifiConnectionReceiver);
        enableWifi(null);
    }

    @Override
    public void disableWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            unregisterReceiver(mContext, mWifiStateReceiver);
            unregisterReceiver(mContext, mWifiScanReceiver);
            unregisterReceiver(mContext, mWifiConnectionReceiver);
        }
        wifiLog("WiFi Disabled");
    }

    @Override
    public WifiInfo getWifiInfo(){
        try {
            return mWifiManager.getConnectionInfo();
        }catch (Exception e){
            return null;
        }
    }
}
