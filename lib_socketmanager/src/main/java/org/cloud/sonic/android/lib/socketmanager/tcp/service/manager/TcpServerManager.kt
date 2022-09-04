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