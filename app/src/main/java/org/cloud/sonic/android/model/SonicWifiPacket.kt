/*
 *  Copyright (C) [SonicCloudOrg] Sonic Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
