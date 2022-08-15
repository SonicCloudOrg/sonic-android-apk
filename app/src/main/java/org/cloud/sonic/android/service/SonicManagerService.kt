/*
 *  Copyright (C) [SonicCloudOrg] Sonic Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.cloud.sonic.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cloud.sonic.android.R
import org.cloud.sonic.android.constants.Contants.ACTION_GET_ALL_APP_INFO
import org.cloud.sonic.android.constants.Contants.ACTION_GET_ALL_WIFI_INFO
import org.cloud.sonic.android.plugin.SonicPluginAppList
import org.cloud.sonic.android.plugin.SonicPluginWifiManager
import org.cloud.sonic.android.utils.appGlobalScope
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

//@AndroidEntryPoint
class SonicManagerService : Service() {

  companion object {
    fun start(context: Context) {
      val intent = Intent(context, SonicManagerService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }
  }

  private val CHANNEL_ID = "sonic_manager_notification_channel_id_01"
  private val ACTION_STOP = "org.cloud.sonic.android.STOP"
  private val NOTIFICATION_ID = 1
  private var serviceIsLive = false

  private val SONIC_MANAGER_SOCKET = "sonicmanagersocket"
  private lateinit var serverSocket: LocalServerSocket
  private lateinit var clientSocket: LocalSocket

  private lateinit var outputStream: OutputStream
  private lateinit var inputStream: InputStream
  //hilt 依赖注解
//    @Inject lateinit var appListPlugin:SonicPluginAppList
//    @Inject lateinit var wifiManager:SonicPluginWifiManager

  lateinit var appListPlugin: SonicPluginAppList
  lateinit var wifiManager: SonicPluginWifiManager

  private val REC_SERVICE_ACTION = 1

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
      clientSocket = serverSocket.accept()
      LogUtils.d("client connected")
      outputStream = clientSocket.outputStream
      acceptMsg()
      closeSocket()
      stopSelf()
    }
    return super.onStartCommand(intent, flags, startId)
  }

  fun closeSocket() {
    try {
      serverSocket.close()
    } catch (e: IOException) {
      println(e.message)
      e.printStackTrace()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    LogUtils.i("onDestroy")
    // 标记服务关闭
    serviceIsLive = false;
    // 移除通知
    stopForeground(true);
    super.onDestroy();
  }

  private fun getNotificationManager(): NotificationManager {
    return getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  }

  private fun acceptMsg() {
    while (true) {
      try {
        val buffer = ByteArray(1024)
        inputStream = clientSocket.inputStream
        val count = inputStream.read(buffer)
        val key = String(Arrays.copyOfRange(buffer, 0, count))
        LogUtils.d("ServerActivity mSocketOutStream==$key")
        val msg: Message = mHandler.obtainMessage(REC_SERVICE_ACTION)
        msg.obj = key
        msg.sendToTarget()
      } catch (e: IOException) {
        LogUtils.d("exception==" + e.fillInStackTrace().message)
        e.printStackTrace()
      }
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
      REC_SERVICE_ACTION -> {
        val recMes = msg.obj as String
        when (recMes) {
          ACTION_STOP -> stopSelf()
          ACTION_GET_ALL_APP_INFO -> appListPlugin.getAllAppInfo(outputStream = outputStream)
          ACTION_GET_ALL_WIFI_INFO -> wifiManager.getAllWifiList(outputStream = outputStream)
        }
        if (ACTION_STOP == recMes) {
          stopSelf()
        }
      }
      else -> {
        Log.e("ManagerService", "why are you here?")
      }
    }
  }
}
