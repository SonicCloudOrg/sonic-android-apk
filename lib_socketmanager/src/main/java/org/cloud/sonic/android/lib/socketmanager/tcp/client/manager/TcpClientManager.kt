/*
 *
 * Copyright (C) [SonicCloudOrg] Sonic Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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