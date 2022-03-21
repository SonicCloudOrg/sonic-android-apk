package org.cloud.sonic.android.models;

import java.util.List;

public class WifiPacket {
    boolean isConnectWifi;
    SonicWifiInfo connectedWifi;
    List<SonicWifiInfo> wifiResults;

    public boolean isConnectWifi() {
        return isConnectWifi;
    }

    public void setConnectWifi(boolean connectWifi) {
        isConnectWifi = connectWifi;
    }

    public SonicWifiInfo getConnectedWifi() {
        return connectedWifi;
    }

    public void setConnectedWifi(SonicWifiInfo connectedWifi) {
        this.connectedWifi = connectedWifi;
    }

    public List<SonicWifiInfo> getWifiResults() {
        return wifiResults;
    }

    public void setWifiResults(List<SonicWifiInfo> wifiResults) {
        this.wifiResults = wifiResults;
    }
}

