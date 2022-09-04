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

package org.cloud.sonic.android.lib.socketmanager.tcp.client.listener

import org.cloud.sonic.android.lib.socketmanager.tcp.client.TcpClient
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TcpMassage

interface TcpClientListener {
  fun onConnected(client: TcpClient)

  fun onSent(client: TcpClient, tcpMsg: TcpMassage)

  fun onDisconnected(client: TcpClient, msg: String, e: Exception?)

  fun onReceive(client: TcpClient, tcpMsg: TcpMassage)

  fun onValidationFail(client: TcpClient, tcpMsg: TcpMassage)
}
