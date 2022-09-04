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

package org.cloud.sonic.android.lib.socketmanager.tcp.listener

import org.cloud.sonic.android.lib.socketmanager.tcp.client.TcpClient
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TcpMassage
import org.cloud.sonic.android.lib.socketmanager.tcp.service.TcpServer

interface TcpServerListener {
    fun onCreated(server: TcpServer)

    fun onListened(server: TcpServer)

    fun onAccept(server: TcpServer, tcpClient: TcpClient)

    fun onSent(server: TcpServer, tcpClient: TcpClient, tcpMsg: TcpMassage)

    fun onReceive(server: TcpServer, tcpClient: TcpClient, tcpMsg: TcpMassage)

    fun onValidationFail(server: TcpServer, client: TcpClient, tcpMsg: TcpMassage)

    fun onClientClosed(server: TcpServer, tcpClient: TcpClient, msg: String, e: Exception?)

    fun onServerClosed(server: TcpServer, msg: String?, e: Exception)
    
}