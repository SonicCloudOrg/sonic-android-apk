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

package org.cloud.sonic.android.lib.socketmanager.tcp.client.manager

import org.cloud.sonic.android.lib.socketmanager.tcp.client.TcpClient
import org.cloud.sonic.android.lib.socketmanager.tcp.client.state.ClientState
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TargetInfo
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpConnConfig
import org.cloud.sonic.android.lib.socketmanager.utils.ExceptionUtils
import java.net.Socket

/**
 * tcpclient的管理者
 */
object TcpClientManager {
    private val sMTcpClients: MutableSet<TcpClient> = HashSet<TcpClient>()

    private fun putTcpClient(tcpClient: TcpClient) {
        sMTcpClients.add(tcpClient)
    }

    public fun removeTcpClient(tcpClient: TcpClient){
        sMTcpClients.remove(tcpClient)
    }

    fun getTcpClient(targetInfo: TargetInfo, tcpConnConfig: TcpConnConfig?): TcpClient {
        for (tc in sMTcpClients) {
            if (tc.getTargetInfo().equals(targetInfo)) {
                return tc
            }
        }

        val tcpClient = TcpClient()
        tcpClient.init(targetInfo, tcpConnConfig)
        putTcpClient(tcpClient)
        return tcpClient
    }

    fun getTcpClient(socket: Socket, targetInfo: TargetInfo, tcpConnConfig: TcpConnConfig?): TcpClient {
        if (!socket.isConnected) {
            ExceptionUtils.throwException("socket is closeed")
        }
        val tcpClient = TcpClient()
        tcpClient.init(targetInfo, tcpConnConfig)
        tcpClient.mSocket = socket
        tcpClient.mClientState = ClientState.Connected
        tcpClient.onConnectSuccess()
        return tcpClient
    }
}