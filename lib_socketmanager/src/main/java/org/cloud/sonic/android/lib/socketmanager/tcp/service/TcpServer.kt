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
package org.cloud.sonic.android.lib.socketmanager.tcp.service

import org.cloud.sonic.android.lib.socketmanager.tcp.client.TcpClient
import org.cloud.sonic.android.lib.socketmanager.tcp.client.listener.TcpClientListener
import org.cloud.sonic.android.lib.socketmanager.tcp.listener.TcpServerListener
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TargetInfo
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TcpMassage
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpServerConfig
import org.cloud.sonic.android.lib.socketmanager.tcp.service.manager.TcpServerManager
import org.cloud.sonic.android.lib.socketmanager.tcp.service.state.ServerState
import org.cloud.sonic.android.lib.socketmanager.utils.SonicSocketLog
import org.cloud.sonic.android.lib.socketmanager.utils.runOnMainThread
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

/**
 * tcp server 实现
 */
class TcpServer : TcpClientListener {
    companion object {
        const val TAG = "TcpService"

        fun getTcpServer(socketPort: Int): TcpServer {
            return TcpServerManager.getTcpServer(socketPort)
        }
    }
    var mSocketName: String = ""
    var mSocketPort: Int = -1;
    var mServerSocket: ServerSocket? = null
    var mListenThread: ListenThread? = null
    var mTcpServerConfig: TcpServerConfig? = null
    lateinit var mMapTcpClients: MutableMap<TargetInfo,TcpClient>
    private var mServerState: ServerState = ServerState.Closed
    private lateinit var mTcpServerListeners: MutableList<TcpServerListener?>

    /**
     * 初始化一个新的 TcpService
     * @param socketPort LocalSocket 的地址名称
     */
    fun init(socketPort: Int) {
        this.mSocketPort = socketPort
        mServerState = ServerState.Closed
        mMapTcpClients = LinkedHashMap()
        mTcpServerListeners = ArrayList()
        if (mTcpServerConfig == null) {
            mTcpServerConfig = TcpServerConfig.Builder().create()
        }
    }

    /**
     * 开启 tcp 的服务
     */
    public fun startServer() {
        if (!getListenThread().isAlive) {
            SonicSocketLog.d(TAG,"tcp service 启动ing")
            getListenThread().start()
        }
    }

    /**
     * 关闭 tcp 的服务
     */
    public fun stopServer() {
        stopServer("主动调用关闭 tcp Server", null)
    }

    private fun stopServer(msg: String, e: Exception?) {
        getListenThread().interrupt() // 主动阻断 listener 的线程
        setServerState(ServerState.Closed)
        if (closeSocket()){
            for (client in mMapTcpClients.values) {
                client.disconnect()
            }
            notifyTcpServerClosed(msg, e)
        }
        SonicSocketLog.d(TAG,"Tcp Server closed")
    }

    private fun closeSocket(): Boolean {
        mServerSocket?.let {
            if (!it.isClosed){
                try {
                    it.close()
                    return true
                } catch (e: IOException){
                    SonicSocketLog.e(TAG,"Socket close error: ${e.message} ")
                }
            }
        }
        return false
    }

    fun sendMsgToAll(msg: TcpMassage): Boolean {
        var re = true
        for (client in mMapTcpClients.values) {
            if (client.sendMsg(msg) == null) {
                re = false
            }
        }
        return re
    }

    fun sendMsgToAll(msg: String): Boolean {
        var re = true
        for (client in mMapTcpClients.values) {
            if (client.sendMsg(msg) == null) {
                re = false
            }
        }
        return re
    }

    fun sendMsgToAll(msg: ByteArray): Boolean {
        var re = true
        for (client in mMapTcpClients.values) {
            if (client.sendMsg(msg) == null) {
                re = false
            }
        }
        return re
    }

    private fun getListenThread(): ListenThread {
        if (mListenThread == null || !mListenThread!!.isAlive) {
            mListenThread = ListenThread()
        }
        return mListenThread!!
    }

    private fun getServerSocket(): ServerSocket {
        if (mServerSocket == null || mServerSocket!!.isClosed()) {
            try {
                mServerSocket = ServerSocket(mSocketPort)
                setServerState(ServerState.Closed)
                notifyTcpServerCreate()
                setServerState(ServerState.Listening)
                notifyTcpServerListen()
            } catch (e: Exception) {
                SonicSocketLog.e(TAG,"创建service 失败，失败原因：${e.message}")
                stopServer("创建失败", e)
            }
        }
        return mServerSocket!!
    }

    override fun onConnected(client: TcpClient) {
        // no callback,ignore
    }

    override fun onSent(client: TcpClient, tcpMsg: TcpMassage) {
        notifyTcpServerSent(client, tcpMsg)
    }

    override fun onDisconnected(client: TcpClient, msg: String, e: Exception?) {
        mMapTcpClients.remove(client.getTargetInfo())
        notifyTcpClientClosed(client, msg, e)
    }

    override fun onReceive(client: TcpClient, tcpMsg: TcpMassage) {
        notifyTcpServerReceive(client, tcpMsg)
    }

    override fun onValidationFail(client: TcpClient, tcpMsg: TcpMassage) {
        notifyTcpServerValidationFail(client, tcpMsg)
    }

    inner class ListenThread : Thread() {
        override fun run() {
            var socket: Socket
            while (!interrupted()) {
                try {
                    SonicSocketLog.d(TAG, "tcp server listening")
                    socket = getServerSocket().accept()
                    val targetInfo = TargetInfo(socket.inetAddress.hostAddress, socket.port)
                    val tcpClient: TcpClient = TcpClient.getTcpClient(
                        socket, targetInfo,
                        mTcpServerConfig?.mTcpConnConfig
                    ) //创建一个client，接受和发送消息
                    notifyTcpServerAccept(tcpClient)
                    tcpClient.addTcpClientListener(this@TcpServer)
                    mMapTcpClients[targetInfo] = tcpClient
                } catch (e: IOException) {
                    SonicSocketLog.d(TAG, "tcp server listening error:$e")
                    stopServer("监听失败", e)
                }
            }
        }
    }

    fun addTcpServerListener(listener: TcpServerListener) {
        if (mTcpServerListeners.contains(listener)) {
            return
        }
        mTcpServerListeners.add(listener)
    }

    fun removeTcpServerListener(listener: TcpServerListener?) {
        mTcpServerListeners.remove(listener)
    }

    private fun notifyTcpServerCreate() {
        for (wr in mTcpServerListeners) {
            runOnMainThread {
                wr?.onCreated(this@TcpServer)
            }
        }
    }

    private fun notifyTcpServerListen() {
        for (wr in mTcpServerListeners) {
            runOnMainThread {
                wr?.onListened(this@TcpServer)
            }
        }
    }

    private fun notifyTcpServerClosed(msg: String, e: java.lang.Exception?) {
        for (wr in mTcpServerListeners) {
            runOnMainThread {
                wr?.onServerClosed(this@TcpServer, msg, e)
            }
        }
    }


    private fun notifyTcpServerLinten() {
        for (wr in mTcpServerListeners) {
            runOnMainThread {
                wr?.onListened(this@TcpServer)
            }
        }
    }

    private fun notifyTcpServerAccept(client: TcpClient) {
        for (wr in mTcpServerListeners) {
            if (wr != null) {
                runOnMainThread {
                    wr.onAccept(this@TcpServer, client)
                }
            }
        }
    }

    private fun notifyTcpServerReceive(client: TcpClient, tcpMsg: TcpMassage) {
        for (wr in mTcpServerListeners) {
            runOnMainThread {
                wr?.onReceive(this@TcpServer, client, tcpMsg)
            }
        }
    }

    private fun notifyTcpServerSent(client: TcpClient, tcpMsg: TcpMassage) {
        for (wr in mTcpServerListeners) {
            runOnMainThread {
                wr?.onSent(this@TcpServer, client, tcpMsg)
            }
        }
    }

    private fun notifyTcpServerValidationFail(client: TcpClient, tcpMsg: TcpMassage) {
        for (wr in mTcpServerListeners!!) {
            runOnMainThread {
                wr?.onValidationFail(this@TcpServer, client, tcpMsg)
            }
        }
    }

    private fun notifyTcpClientClosed(client: TcpClient, msg: String, e: java.lang.Exception?) {
        for (wr in mTcpServerListeners) {
            runOnMainThread {
                wr?.onClientClosed(this@TcpServer, client, msg, e)
            }
        }
    }

    fun getPort(): Int {
        return mSocketPort
    }

    private fun setServerState(state: ServerState) {
        mServerState = state
    }

    fun isClosed(): Boolean {
        return mServerState === ServerState.Closed
    }

    fun isListening(): Boolean {
        return mServerState === ServerState.Listening
    }

    fun config(tcpServerConfig: TcpServerConfig) {
        mTcpServerConfig = tcpServerConfig
    }

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append("Xtcpserver port=$mSocketPort,state=$mServerState")
        sb.append(" client size=" + mMapTcpClients?.size)
        return sb.toString()
    }

}
