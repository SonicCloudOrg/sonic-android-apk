/*
 *  sonic-android-apk  Help your Android device to do more.
 *  Copyright (C) 2022 SonicCloudOrg
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
    var BSSID: String ?= "",
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
