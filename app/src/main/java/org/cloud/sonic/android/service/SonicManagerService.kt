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
import android.graphics.drawable.Icon
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cloud.sonic.android.R
import org.cloud.sonic.android.constants.Contants.ACTION_GET_ALL_APP_INFO
import org.cloud.sonic.android.constants.Contants.ACTION_GET_ALL_WIFI_INFO
import org.cloud.sonic.android.lib.socketmanager.tcp.client.TcpClient
import org.cloud.sonic.android.lib.socketmanager.tcp.client.listener.TcpClientListener
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TcpMassage
import org.cloud.sonic.android.plugin.SonicPluginAppList
import org.cloud.sonic.android.plugin.SonicPluginWifiManager
import org.cloud.sonic.android.utils.appGlobalScope
import java.io.*
import java.util.*

//@AndroidEntryPoint
class SonicManagerService : Service() {

  companion object {
    private const val CHANNEL_ID = "sonic_manager_notification_channel_id_01"
    private const val ACTION_STOP = "org.cloud.sonic.android.STOP"
    private const val SONIC_MANAGER_SOCKET = "sonicmanagersocket"
    private const val NOTIFICATION_ID = 1
    private const val REC_SERVICE_ACTION = 1
    private const val LINK_SOCKET_TIMEOUT = 30 * 1000
    private const val LINK_SOCKET_TIMEOUT_MSG = 0

    fun start(context: Context) {
      val intent = Intent(context, SonicManagerService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }
  }

  private var serviceIsLive = false
  private var isSocketStop = false

  private var serverSocket: LocalServerSocket? = null
  private var clientSocket: LocalSocket? = null

  private var outputStream: OutputStream? = null

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

  override fun onCreate() {
    super.onCreate()
    LogUtils.i("onCreate")
    appListPlugin = SonicPluginAppList(this)
    wifiManager = SonicPluginWifiManager(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val notification = createNotificationWithAndroidO(false)
      val channel = NotificationChannel(
        CHANNEL_ID,
        getString(R.string.app_name),
        NotificationManager.IMPORTANCE_NONE
      )
      getNotificationManager().createNotificationChannel(channel)
      startForeground(
        NOTIFICATION_ID,
        notification
      )
    } else {
      // 获取服务通知
      val notification: Notification = createNotification(false)
      //将服务置于启动状态 ,NOTIFICATION_ID指的是创建的通知的ID
      startForeground(NOTIFICATION_ID, notification)
    }
  }


  override fun onBind(p0: Intent?): IBinder? {
    LogUtils.i("OnBind")
    return null
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    LogUtils.i("onStartCommand")
    val action = intent!!.action
    if (ACTION_STOP == action) {
      closeSocket()
      //exit
      stopSelf()
      return START_NOT_STICKY
    }
    // 标记服务启动
    serviceIsLive = true
    appGlobalScope.launch(Dispatchers.IO) {
      LogUtils.i(String.format("creating socket %s", SONIC_MANAGER_SOCKET))
      serverSocket = LocalServerSocket(SONIC_MANAGER_SOCKET)
      LogUtils.i(String.format("Listening on %s", SONIC_MANAGER_SOCKET))
      clientSocket = serverSocket?.accept()
      LogUtils.d("client connected")
      outputStream = clientSocket?.outputStream
      acceptMsg()
    }
    linkTimeOutStop()

    return super.onStartCommand(intent, flags, startId)
  }

  private fun linkTimeOutStop() {
    val msg = mHandler.obtainMessage(
      LINK_SOCKET_TIMEOUT_MSG
    )
    msg.obj = "LINK_SOCKET_TIMEOUT"
    mHandler.sendMessageDelayed(
      msg,
      LINK_SOCKET_TIMEOUT.toLong()
    )
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

  private fun acceptMsg() {
    isSocketStop = true
    try {
      BufferedReader(InputStreamReader(clientSocket?.inputStream)).use { read ->
        val key = read.readLine()
        key?.let {
          do {
            val msg: Message = mHandler.obtainMessage(REC_SERVICE_ACTION)
            msg.obj = key
            msg.sendToTarget()
          } while (isSocketStop)
        }
      }
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationWithAndroidO(established: Boolean): Notification {
    val notificationBuilder =
      Notification.Builder(this, CHANNEL_ID)
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
    val intent = Intent(this, SonicManagerService::class.java)
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
        stopSelf()
      }
      REC_SERVICE_ACTION -> {
        when (msg.obj as String) {
          ACTION_STOP -> {
            closeSocket()
            stopSelf()
          }
          ACTION_GET_ALL_APP_INFO -> appListPlugin.getAllAppInfo(outputStream = outputStream)
          ACTION_GET_ALL_WIFI_INFO -> wifiManager.getAllWifiList(outputStream = outputStream)
          else -> LogUtils.w("service action is ${msg.obj}")
        }
      }
    }
  }

  private fun closeSocket() {
    isSocketStop = true
    outputStream?.close()
    serverSocket?.close()
    clientSocket?.close()
    outputStream = null
    serverSocket = null
    clientSocket = null
  }
}
