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
package org.cloud.sonic.android.plugin

import android.content.Context
import android.net.wifi.WifiManager
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.thanosfisherman.wifiutils.WifiUtils
import org.cloud.sonic.android.lib.socketmanager.tcp.service.TcpServer
import org.cloud.sonic.android.model.SonicWifiInfo
import org.cloud.sonic.android.model.SonicWifiPacket
import java.io.IOException
import java.io.OutputStream

class SonicPluginWifiManager constructor(
    private val context: Context
) :IPlugin {
    fun getAllWifiList(outputStream: OutputStream?) {
        WifiUtils.withContext(context).scanWifi { results ->
            val mWifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = mWifiManager.connectionInfo
            val wifiInfos: MutableList<SonicWifiInfo> = ArrayList()

            if (results.isEmpty()) {
                LogUtils.i("SCAN RESULTS IT'S EMPTY")
            } else {
                LogUtils.i("GOT SCAN RESULTS " + results)
                for (result in results) {
                    wifiInfos.add(SonicWifiInfo.transform(result))
                }
            }

            val sendPacket = SonicWifiPacket(
                isConnectWifi = NetworkUtils.isWifiConnected(),
                connectedWifi = SonicWifiInfo.transform(wifiInfo),
                wifiResults = wifiInfos
            )

            try {
                val dataBytes: ByteArray = GsonUtils.toJson(sendPacket).toByteArray()
                // 数据长度转成二进制，存入byte[32]
                val lengthBytes = ByteArray(32)
                val binStr = Integer.toBinaryString(dataBytes.size).trim { it <= ' ' }
                val binArray = binStr.toCharArray()
                var x = binArray.size - 1
                var y = lengthBytes.size - 1
                while (x >= 0) {
                    try {
                        lengthBytes[y] = (binArray[x].toString() + "").toByte()
                    } catch (e: Exception) {
                        LogUtils.e(
                            String.format(
                                "char转byte失败，char为：【%s】",
                                binArray[x].toString() + ""
                            )
                        )
                    }
                    x--
                    y--
                }
                // 先发送长度
                outputStream?.write(lengthBytes)
                outputStream?.flush()

                // 再发送数据
                outputStream?.write(dataBytes)
                outputStream?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start();
    }

    fun getAllWifiList(sonicTcpServer: TcpServer?) {
        WifiUtils.withContext(context).scanWifi { results ->
            val mWifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = mWifiManager.connectionInfo
            val wifiInfos: MutableList<SonicWifiInfo> = ArrayList()

            if (results.isEmpty()) {
                LogUtils.i("SCAN RESULTS IT'S EMPTY")
            } else {
                LogUtils.i("GOT SCAN RESULTS " + results)
                for (result in results) {
                    wifiInfos.add(SonicWifiInfo.transform(result))
                }
            }

            val sendPacket = SonicWifiPacket(
                isConnectWifi = NetworkUtils.isWifiConnected(),
                connectedWifi = SonicWifiInfo.transform(wifiInfo),
                wifiResults = wifiInfos
            )

            try {
                val dataString: String = GsonUtils.toJson(sendPacket)
                sonicTcpServer?.sendMsgToAll(dataString + "\n")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start();
    }
    override fun initPlugin(context: Context) {

    }
}
