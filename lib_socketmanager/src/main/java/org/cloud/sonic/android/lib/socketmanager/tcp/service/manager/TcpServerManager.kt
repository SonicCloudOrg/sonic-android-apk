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

package org.cloud.sonic.android.lib.socketmanager.tcp.service.manager

import org.cloud.sonic.android.lib.socketmanager.tcp.service.TcpServer

object TcpServerManager {
    private val sSetTcpServices = HashSet<TcpServer>()

    fun putTcpServer(tcpServer: TcpServer) {
        sSetTcpServices.add(tcpServer)
    }

    fun getTcpServer(tcpPort: Int): TcpServer {
        for (ts in sSetTcpServices) {
            if (ts.mSocketPort == tcpPort) {
                return ts
            }
        }
        val ts = TcpServer()
        ts.init(tcpPort)
        putTcpServer(ts)
        return ts
    }
}