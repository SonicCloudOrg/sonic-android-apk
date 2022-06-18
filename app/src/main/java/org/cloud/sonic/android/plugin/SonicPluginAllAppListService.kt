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
package org.cloud.sonic.android.plugin

import android.content.Context
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.lang.reflect.Method


class SonicPluginAllAppListService(var handler: Handler?) : Thread() {
  lateinit var appListPlugin: SonicPluginAppList

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      try {
        Looper.prepare()
        val handler = Handler()
        val m = SonicPluginAllAppListService(handler)
        m.start()
        println("SonicPluginAllAppListService starting：start()")
        Looper.loop()
      } catch (e: InterruptedException) {
        println("ERROR:${e.message}")
      }
    }
  }

  private fun getContext(): Context {
    val activityThread = Class.forName("android.app.ActivityThread")
    val systemMain: Method = activityThread.getDeclaredMethod("systemMain")
    val objectSystemMain: Any = systemMain.invoke(null)
    val contextImpl = Class.forName("android.app.ContextImpl")
    val createSystemContext: Method =
      contextImpl.getDeclaredMethod("createSystemContext", activityThread)
    createSystemContext.isAccessible = true
    val contextInstance: Context = createSystemContext.invoke(null, objectSystemMain) as Context
    return contextInstance.createPackageContext(
      "org.cloud.sonic.android.app",
      Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
    )
  }

  private var serverSocket: LocalServerSocket? = null
  private val SOCKET = "sonic_plugin_all_app_list_service"

  init {
    appListPlugin = SonicPluginAppList(getContext())
  }


  override fun run() {

    try {
      println(String.format("creating socket %s", SOCKET))
      serverSocket = LocalServerSocket(SOCKET)
    } catch (e: IOException) {
      println(e.message)
      e.printStackTrace()
      return
    }

    manageClientConnection()

    try {
      serverSocket?.close()
    } catch (e: IOException) {
      println(e.message)
      e.printStackTrace()
    }
    println("socket closed.")
    System.exit(0)
  }

  private fun manageClientConnection() {
    println(String.format("Listening on %s", SOCKET))
    val clientSocket: LocalSocket?
    try {
      clientSocket = serverSocket!!.accept()
      processCommandLoop(clientSocket)
      println("client connected")
    } catch (e: IOException) {
      println("error")
    }
  }

  @Throws(IOException::class)
  private fun processCommandLoop(clientSocket: LocalSocket) {
    appListPlugin.getAllAppInfo(outputStream = clientSocket.outputStream)
  }
}
