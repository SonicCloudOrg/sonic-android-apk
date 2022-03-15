package org.cloud.sonic.android.models;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.support.v4.app.ActivityCompat;

public class SonicWifiInfo {
    String SSID = "";
    String BSSID = "";
    String capabilities = "";
    String ipAddress = "";
    String macAddress = "";

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public String getBSSID() {
        return BSSID;
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    @SuppressLint("MissingPermission")
    public static SonicWifiInfo transform(WifiInfo wifiInfo) {
        SonicWifiInfo info = new SonicWifiInfo();
        info.setSSID(wifiInfo.getSSID());
        info.setBSSID(wifiInfo.getBSSID());
        info.setIpAddress(wifiInfo.getIpAddress() + "");
        info.setMacAddress(wifiInfo.getMacAddress());
        return info;
    }

    @SuppressLint("MissingPermission")
    public static SonicWifiInfo transform(ScanResult scanResult) {
        SonicWifiInfo info = new SonicWifiInfo();
        info.setSSID(scanResult.SSID);
        info.setBSSID(scanResult.BSSID);
        info.setCapabilities(scanResult.capabilities);
        return info;
    }
}
