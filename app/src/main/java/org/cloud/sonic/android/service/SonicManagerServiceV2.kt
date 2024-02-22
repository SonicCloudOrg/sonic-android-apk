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
package org.cloud.sonic.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.LogUtils
import org.cloud.sonic.android.R
import org.cloud.sonic.android.lib.socketmanager.tcp.client.TcpClient
import org.cloud.sonic.android.lib.socketmanager.tcp.listener.TcpServerListener
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TcpMassage
import org.cloud.sonic.android.lib.socketmanager.tcp.service.TcpServer
import org.cloud.sonic.android.lib.socketmanager.tcp.service.TcpServer.Companion.getTcpServer
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpConnConfig
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpServerConfig
import org.cloud.sonic.android.lib.socketmanager.utils.CharsetUtil
import org.cloud.sonic.android.plugin.SonicPluginAppList
import org.cloud.sonic.android.plugin.SonicPluginWifiManager

//@AndroidEntryPoint
class SonicManagerServiceV2 : Service(), TcpServerListener {
    companion object {
        private const val CHANNEL_ID = "sonic_manager_notification_channel_id_01"
        private const val ACTION_STOP = "org.cloud.sonic.android.STOP"
        private const val ACTION_TIME_OUT = "LINK_SOCKET_TIMEOUT"
        private const val ACTION_GET_ALL_APP_INFO = "action_get_all_app_info"
        private const val ACTION_GET_ALL_WIFI_INFO = "action_get_all_wifi_info"
        private const val SONIC_MANAGER_SOCKET_PORT = "2334"
        private const val NOTIFICATION_ID = 1
        private const val REC_SERVICE_ACTION = 1
        private const val LINK_SOCKET_TIMEOUT = 30 * 1000
        private const val LINK_SOCKET_TIMEOUT_MSG = 0

        fun start(context: Context) {
            val intent = Intent(context, SonicManagerServiceV2::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private var serviceIsLive = false
    private var isSocketStop = false
    private var mSonicTcpServer: TcpServer? = null

    //hilt 依赖注解
    //@Inject lateinit var appListPlugin:SonicPluginAppList
    //@Inject lateinit var wifiManager:SonicPluginWifiManager

    lateinit var appListPlugin: SonicPluginAppList
    lateinit var wifiManager: SonicPluginWifiManager

    var mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            processOrder(msg)
        }
    }

    fun StartForeground(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        LogUtils.i("onCreate")
        appListPlugin = SonicPluginAppList(this)
        wifiManager = SonicPluginWifiManager(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = createNotificationWithAndroidO(false)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_NONE
            )
            getNotificationManager().createNotificationChannel(channel)
            StartForeground(NOTIFICATION_ID, notification)
        } else {
            // 获取服务通知
            val notification: Notification = createNotification(false)
            //将服务置于启动状态 ,NOTIFICATION_ID指的是创建的通知的ID
            StartForeground(NOTIFICATION_ID, notification)
        }
    }


    override fun onBind(p0: Intent?): IBinder? {
        LogUtils.i("OnBind")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.i("onStartCommand")
        val action = intent?.action
        if (ACTION_STOP == action) {
            closeSocket()
            return START_NOT_STICKY
        }
        // 标记服务启动
        serviceIsLive = true
        //启动Socket服务
        startSocket()
        linkTimeOutStop()
        return super.onStartCommand(intent, START_REDELIVER_INTENT, startId)
    }

    private fun linkTimeOutStop() {
        val msg = mHandler.obtainMessage(LINK_SOCKET_TIMEOUT_MSG)
        msg.obj = ACTION_TIME_OUT
        mHandler.sendMessageDelayed(msg, LINK_SOCKET_TIMEOUT.toLong())
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.i("onDestroy")
        // 标记服务关闭
        serviceIsLive = false;
        // 移除通知
        stopForeground(true)
        closeSocket()
        super.onDestroy();
    }

    private fun getNotificationManager(): NotificationManager {
        return getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationWithAndroidO(established: Boolean): Notification {
        val notificationBuilder = Notification.Builder(this, CHANNEL_ID)
        notificationBuilder.setContentTitle(getString(R.string.welcome))
        val textRes: Int =
            if (established) R.string.manager_notification_forwarding else R.string.notification_waiting
        notificationBuilder.setContentText(getText(textRes))
        notificationBuilder.setSmallIcon(R.mipmap.logo)
        notificationBuilder.addAction(createStopActionWithAndroidO())
        return notificationBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createStopActionWithAndroidO(): Notification.Action {
        val stopIntent: Intent = createStopIntent()
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIcon = Icon.createWithResource(this, R.mipmap.logo)
        val stopString = getString(R.string.action_stop)
        val actionBuilder = Notification.Action.Builder(stopIcon, stopString, stopPendingIntent)
        return actionBuilder.build()
    }

    private fun createStopIntent(): Intent {
        val intent = Intent(this, SonicManagerServiceV2::class.java)
        intent.action = ACTION_STOP
        return intent
    }

    private fun createNotification(established: Boolean): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        notificationBuilder.setContentTitle(getString(R.string.app_name))
        val textRes: Int =
            if (established) R.string.manager_notification_forwarding else R.string.notification_waiting
        notificationBuilder.setSmallIcon(R.mipmap.logo)
        notificationBuilder.setContentText(getText(textRes))
        notificationBuilder.addAction(createStopAction())
        return notificationBuilder.build()
    }

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent: Intent = createStopIntent()
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIcon = R.mipmap.logo
        val stopString = getString(R.string.action_stop)
        val actionBuilder =
            NotificationCompat.Action.Builder(stopIcon, stopString, stopPendingIntent)
        return actionBuilder.build()
    }

    private fun processOrder(msg: Message) {
        when (msg.what) {
            LINK_SOCKET_TIMEOUT_MSG -> {
                closeSocket()
            }
            REC_SERVICE_ACTION -> {

            }
        }
    }

    private fun startSocket() {
        if (mSonicTcpServer == null) {
            mSonicTcpServer = getTcpServer(SONIC_MANAGER_SOCKET_PORT.toInt())
            mSonicTcpServer?.let {
                it.addTcpServerListener(this)
                it.config(
                    TcpServerConfig.Builder()
                        .setTcpConnConfig(TcpConnConfig.Builder().create()).create()
                )
            }
        }
        mSonicTcpServer?.startServer()
    }

    private fun closeSocket() {
        isSocketStop = true
        mSonicTcpServer?.let {
            if (it.isListening()) {
                it.removeTcpServerListener(this)
                it.stopServer()
            }
        }
        mSonicTcpServer = null
        stopSelf()
    }

    private fun processReceiveMsg(msg: String) {
        when (msg) {
            ACTION_STOP -> closeSocket()
            ACTION_GET_ALL_APP_INFO -> appListPlugin.getAllAppInfo(mSonicTcpServer)
            ACTION_GET_ALL_WIFI_INFO -> wifiManager.getAllWifiList(mSonicTcpServer)
            else -> LogUtils.w("service action is $msg")
        }
    }

    override fun onCreated(server: TcpServer) {
        LogUtils.d("服务启动成功")
    }

    override fun onListened(server: TcpServer) {
        LogUtils.d("服务 listenling ${server.getPort()}")
    }

    override fun onAccept(server: TcpServer, tcpClient: TcpClient) {
        LogUtils.d("收到客户端连接请求 ${tcpClient.getTargetInfo().ip}")
        mHandler.removeMessages(LINK_SOCKET_TIMEOUT_MSG)
    }

    override fun onSent(server: TcpServer, tcpClient: TcpClient, tcpMsg: TcpMassage) {
        LogUtils.d("发送消息给 ${tcpClient.getTargetInfo().ip} 成功 msg= ${tcpMsg.getSourceDataString()}")
    }

    override fun onReceive(server: TcpServer, tcpClient: TcpClient, tcpMsg: TcpMassage) {
        LogUtils.d(
            "收到客户端消息 ${tcpClient.getTargetInfo().ip} , ${
                CharsetUtil.dataToString(
                    tcpMsg.getSourceDataBytes(),
                    CharsetUtil.UTF_8
                )
            }"
        )
        //处理收到消息
        val msg = CharsetUtil.dataToString(
            tcpMsg.getSourceDataBytes(),
            CharsetUtil.UTF_8
        )
        processReceiveMsg(msg)
    }

    override fun onValidationFail(server: TcpServer, client: TcpClient, tcpMsg: TcpMassage) {}

    override fun onClientClosed(
        server: TcpServer,
        tcpClient: TcpClient,
        msg: String,
        e: Exception?
    ) {
        LogUtils.d("客户端连接断开 ${tcpClient.getTargetInfo().ip}$msg$e")
        closeSocket()
    }

    override fun onServerClosed(server: TcpServer, msg: String?, e: Exception?) {
        LogUtils.d("服务器关闭 $server$msg$e")
    }
}
