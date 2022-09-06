package org.cloud.sonic.android.lib.socketmanager.tcp.client

import org.cloud.sonic.android.lib.socketmanager.tcp.client.listener.TcpClientListener
import org.cloud.sonic.android.lib.socketmanager.tcp.client.manager.TcpClientManager
import org.cloud.sonic.android.lib.socketmanager.tcp.client.state.ClientState
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TargetInfo
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TcpMassage
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpConnConfig
import org.cloud.sonic.android.lib.socketmanager.utils.CharsetUtil
import org.cloud.sonic.android.lib.socketmanager.utils.SonicSocketLog
import org.cloud.sonic.android.lib.socketmanager.utils.runOnMainThread
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class TcpClient {
    companion object {
        const val TAG = "TcpClient"

        /**
         * 创建 tcp 连接，提供连接的信息
         */
        fun getTcpClient(targetInfo: TargetInfo): TcpClient {
            return TcpClientManager.getTcpClient(targetInfo, null)
        }

        fun getTcpClient(targetInfo: TargetInfo,  tcpConnConfig: TcpConnConfig): TcpClient {
            return TcpClientManager.getTcpClient(targetInfo, tcpConnConfig)
        }

        fun getTcpClient(socket: Socket, targetInfo: TargetInfo, tcpConnConfig: TcpConnConfig?): TcpClient {
            return TcpClientManager.getTcpClient(socket,targetInfo, tcpConnConfig)
        }
    }

    protected var mTargetInfo: TargetInfo? = null //目标ip和端口号
    var mSocket: Socket? = null
    var mClientState: ClientState? = null
    protected var mTcpConnConfig: TcpConnConfig? = null
    protected var mConnectionThread: ConnectionThread? = null
    protected var mSendThread: SentThread? = null
    protected var mReceiveThread: ReceiveThread? = null
    protected lateinit var mTcpClientListeners: MutableList<TcpClientListener>
    private var msgQueue: LinkedBlockingQueue<TcpMassage>? = null

    fun init(targetInfo: TargetInfo, connConfig: TcpConnConfig?) {
        mTargetInfo = targetInfo
        setClientState(ClientState.Disconnected)
        mTcpClientListeners = ArrayList()
        if (mTcpConnConfig == null && connConfig == null) {
            mTcpConnConfig = TcpConnConfig.Builder().create()
        } else if (connConfig != null) {
            mTcpConnConfig = connConfig
        }
    }

    @Synchronized
    fun sendMsg(message: String): TcpMassage? {
        val msg = TcpMassage(message, mTargetInfo, TcpMassage.MsgType.Send)
        return sendMsg(msg)
    }

    @Synchronized
    fun sendMsg(message: ByteArray): TcpMassage? {
        val msg = TcpMassage(message, mTargetInfo, TcpMassage.MsgType.Send)
        return sendMsg(msg)
    }

    @Synchronized
    fun sendMsg(msg: TcpMassage): TcpMassage? {
        if (isDisconnected()) {
            SonicSocketLog.d(TAG, "发送消息 $msg，当前没有tcp连接，先进行连接")
            connect()
        }
        val re: Boolean = enqueueTcpMsg(msg)
        return if (re) {
            msg
        } else null
    }

    @Synchronized
    fun cancelMsg(msg: TcpMassage?): Boolean {
        return getSentThread().cancel(msg)
    }

    @Synchronized
    fun cancelMsg(msgId: Int): Boolean {
        return getSentThread().cancel(msgId)
    }

    @Synchronized
    fun connect() {
        if (!isDisconnected()) {
            SonicSocketLog.d(TAG, "已经连接了或正在连接")
            return
        }
        SonicSocketLog.d(TcpClient.TAG, "tcp connecting")
        setClientState(ClientState.Connecting) //正在连接
        getConnectionThread().start()
    }

    @Synchronized
    fun getSocket(): Socket {
        if (mSocket == null || isDisconnected() || !mSocket!!.isConnected) {
            mSocket = Socket()
            try {
                mSocket!!.soTimeout = mTcpConnConfig?.receiveTimeout?:0
            } catch (e: SocketException) {
                SonicSocketLog.e(TAG, "get Scoket error: ${e.message}")
            }
        }
        return mSocket!!
    }

    @Synchronized
    fun disconnect() {
        disconnect("手动关闭tcpclient", null)
    }

    @Synchronized
    protected fun onErrorDisConnect(msg: String, e: java.lang.Exception?) {
        if (isDisconnected()) {
            return
        }
        disconnect(msg, e)
        if (mTcpConnConfig!!.isReconnect) { //重连
            connect()
        }
    }

    @Synchronized
    protected fun disconnect(msg: String, e: java.lang.Exception?) {
        if (isDisconnected()) {
            return
        }
        closeSocket()
        getConnectionThread().interrupt()
        getSentThread().interrupt()
        getReceiveThread().interrupt()
        setClientState(ClientState.Disconnected)
        notifyDisconnected(msg, e)
        TcpClientManager.removeTcpClient(this)
        SonicSocketLog.d(TAG, "tcp closed")
    }

    @Synchronized
    private fun closeSocket(): Boolean {
        if (mSocket != null) {
            try {
                mSocket!!.close()
            } catch (e: IOException) {
                SonicSocketLog.e(TAG, "tcp close error: ${e.message}")
            }
        }
        return true
    }

    //连接已经连接，接下来的流程，创建发送和接受消息的线程
    fun onConnectSuccess() {
        SonicSocketLog.d(TAG, "tcp connect 建立成功")
        setClientState(ClientState.Connected) //标记为已连接
        getSentThread().start()
        getReceiveThread().start()
    }

    /**
     * tcp连接线程
     */
    inner class ConnectionThread : Thread() {
        override fun run() {
            try {
                val localPort: Int = mTcpConnConfig!!.localPort
                if (localPort > 0) {
                    if (!getSocket().isBound) {
                        getSocket().bind(InetSocketAddress(localPort))
                    }
                }
                getSocket().connect(InetSocketAddress(mTargetInfo!!.ip, mTargetInfo!!.port), mTcpConnConfig!!.connTimeout)
                SonicSocketLog.d(TAG, "创建连接成功,target=$mTargetInfo, localPort=$localPort")
            } catch (e: Exception) {
                SonicSocketLog.d(TAG, "创建连接失败,target=$mTargetInfo,$e")
                onErrorDisConnect("创建连接失败", e)
                return
            }
            notifyConnected()
            onConnectSuccess()
        }
    }

    private fun enqueueTcpMsg(TcpMassage: TcpMassage?): Boolean {
        SonicSocketLog.d(TAG, "enqueueTcpMsg")

        if (TcpMassage == null || getMsgQueue().contains(TcpMassage)) {
            SonicSocketLog.d(TAG, "enqueueTcpMsg error")
            return false
        }
        try {
            getMsgQueue().put(TcpMassage)
            return true
        } catch (e: InterruptedException) {
            SonicSocketLog.e(TAG, "tcp enqueueTcpMsg error: ${e.message}")
        }
        return false
    }

    protected fun getMsgQueue(): LinkedBlockingQueue<TcpMassage> {
        if (msgQueue == null) {
            msgQueue = LinkedBlockingQueue()
        }
        return msgQueue!!
    }


    inner class SentThread : Thread() {
        private var sendingTcpMassage: TcpMassage? = null
        protected fun setSendingTcpMassage(sendingTcpMassage: TcpMassage): SentThread {
            this.sendingTcpMassage = sendingTcpMassage
            return this
        }

        fun getSendingTcpMassage(): TcpMassage? {
            return sendingTcpMassage
        }

        fun cancel(packet: TcpMassage?): Boolean {
            return getMsgQueue().remove(packet)
        }

        fun cancel(TcpMassageID: Int): Boolean {
            return getMsgQueue().remove(TcpMassage(TcpMassageID))
        }

        override fun run() {
            var msg: TcpMassage? = null
            try {
            while (isConnected() && !interrupted() && getMsgQueue().take().also {
                    msg = it
                } != null) {
                    msg?.let {
                        setSendingTcpMassage(it) //设置正在发送的
                        SonicSocketLog.d(TAG, "tcp sending msg=$it")
                        var data: ByteArray? = it.getSourceDataBytes()
                        if (data == null) { //根据编码转换消息
                            data = CharsetUtil.stringToData(
                                it.getSourceDataString(),
                                mTcpConnConfig?.charsetName
                            )
                        }
                        if (data != null && data.isNotEmpty()) {
                            try {
                                getSocket().getOutputStream().write(data)
                                getSocket().getOutputStream().flush()
                                msg?.run {
                                    setTime()
                                    notifySent(this)
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                                onErrorDisConnect("发送消息失败", e)
                                return
                            }
                        }
                    }
                }
            } catch (e: InterruptedException) {
                SonicSocketLog.e(TAG, "sentThread run Error: ${e.message}")
            }
        }
    }

    inner class ReceiveThread : Thread() {
        override fun run() {
            try {
                val `is`: InputStream = getSocket().getInputStream()
                while (isConnected() && !interrupted()) {
                    val result: ByteArray? = mTcpConnConfig?.stickPackageHelper?.execute(`is`) //粘包处理
                    if (result == null) { //报错
                        SonicSocketLog.d(TAG, "tcp Receive 粘包处理失败")
                        onErrorDisConnect("粘包处理中发送错误", null)
                        break
                    }
                    SonicSocketLog.d(TAG, "tcp Receive 解决粘包之后的数据 " + Arrays.toString(result))
                    val TcpMassage = TcpMassage(result, mTargetInfo, TcpMassage.MsgType.Receive)
                    TcpMassage.setTime()
                    val msgstr: String = CharsetUtil.dataToString(result, mTcpConnConfig?.charsetName)
                    TcpMassage.setSourceDataString(msgstr)
                    val va: Boolean = mTcpConnConfig!!.validationHelper.execute(result)
                    if (!va) {
                        SonicSocketLog.d(TAG, "tcp Receive 数据验证失败 ")
                        notifyValidationFail(TcpMassage) //验证失败
                        continue
                    }
                    val decodebytes: Array<ByteArray> = mTcpConnConfig!!.decodeHelper.execute(result, mTargetInfo, mTcpConnConfig)
                    TcpMassage.setEndDecodeData(decodebytes)
                    SonicSocketLog.d(TAG, "tcp Receive  succ msg= $TcpMassage")
                    notifyReceive(TcpMassage) //notify listener
                }
            } catch (e: java.lang.Exception) {
                SonicSocketLog.d(TAG, "tcp Receive  error  $e")
                onErrorDisConnect("接受消息错误", e)
            }
        }
    }

    protected fun getReceiveThread(): ReceiveThread {
        if (mReceiveThread == null || !mReceiveThread!!.isAlive) {
            mReceiveThread = ReceiveThread()
        }
        return mReceiveThread!!
    }

    protected fun getSentThread(): SentThread {
        if (mSendThread == null || !mSendThread!!.isAlive) {
            mSendThread = SentThread()
        }
        return mSendThread!!
    }

    protected fun getConnectionThread(): ConnectionThread {
        if (mConnectionThread == null || !mConnectionThread!!.isAlive || mConnectionThread!!.isInterrupted) {
            mConnectionThread = ConnectionThread()
        }
        return mConnectionThread!!
    }

    fun getClientState(): ClientState {
        return mClientState!!
    }

    protected fun setClientState(state: ClientState) {
        if (mClientState !== state) {
            mClientState = state
        }
    }

    fun isDisconnected(): Boolean {
        return getClientState() === ClientState.Disconnected
    }

    fun isConnected(): Boolean {
        return getClientState() === ClientState.Connected
    }

    private fun notifyConnected() {
        for (wl in mTcpClientListeners) {
            runOnMainThread {wl.onConnected(this@TcpClient)}
        }
    }

    private fun notifyDisconnected(msg: String, e: java.lang.Exception?) {
        for (wl in mTcpClientListeners) {
            runOnMainThread{ wl.onDisconnected(this@TcpClient, msg, e) }
        }
    }

    private fun notifyReceive(TcpMassage: TcpMassage) {
        var l: TcpClientListener
        for (wl in mTcpClientListeners) {
            runOnMainThread{ wl.onReceive(this@TcpClient, TcpMassage) }
        }
    }

    private fun notifySent(TcpMassage: TcpMassage) {
        var l: TcpClientListener
        for (wl in mTcpClientListeners) {
            runOnMainThread { wl.onSent(this@TcpClient, TcpMassage) }
        }
    }

    private fun notifyValidationFail(TcpMassage: TcpMassage) {
        var l: TcpClientListener
        for (wl in mTcpClientListeners) {
            runOnMainThread { wl.onValidationFail(this@TcpClient, TcpMassage) }
        }
    }

    fun getTargetInfo(): TargetInfo {
        return mTargetInfo!!
    }

    fun addTcpClientListener(listener: TcpClientListener?) {
        if (mTcpClientListeners.contains(listener!!)) {
            return
        }
        mTcpClientListeners.add(listener)
    }

    fun removeTcpClientListener(listener: TcpClientListener?) {
        mTcpClientListeners.remove(listener)
    }

    fun config(tcpConnConfig: TcpConnConfig) {
        mTcpConnConfig = tcpConnConfig
    }

    override fun toString(): String {
        return "TcpClient{" +
                "mTargetInfo=" + mTargetInfo + ",state=" + mClientState + ",isconnect=" + isConnected() +
                '}'
    }


}
