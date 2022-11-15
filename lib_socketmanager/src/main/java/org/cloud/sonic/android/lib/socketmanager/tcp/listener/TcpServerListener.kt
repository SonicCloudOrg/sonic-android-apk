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

    fun onServerClosed(server: TcpServer, msg: String?, e: Exception?)
    
}