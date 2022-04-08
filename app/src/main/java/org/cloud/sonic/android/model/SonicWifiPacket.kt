package org.cloud.sonic.android.model

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo

data class SonicWifiPacket(
    var isConnectWifi: Boolean = false,
    var connectedWifi: SonicWifiInfo,
    var wifiResults: List<SonicWifiInfo>,
)

data class SonicWifiInfo(
    var SSID: String = "",
    var BSSID: String = "",
    var capabilities: String = "",
    var ipAddress: String = "",
    var macAddress: String = ""
){
    companion object{
        @SuppressLint("MissingPermission", "HardwareIds")
        fun transform(wifiInfo: WifiInfo):SonicWifiInfo{
            val info = SonicWifiInfo(
                wifiInfo.ssid,
                wifiInfo.bssid,
                wifiInfo.ipAddress.toString(),
                wifiInfo.macAddress.toString()
            )
            return info
        }

        fun transform(scanResult: ScanResult):SonicWifiInfo{
            val info = SonicWifiInfo(
                scanResult.SSID,
                scanResult.BSSID,
                scanResult.capabilities
            )
            return info
        }
    }
}