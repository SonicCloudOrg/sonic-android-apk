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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.LogUtils
import org.cloud.sonic.android.model.SonicAppInfo
import java.io.IOException
import java.io.OutputStream

class SonicPluginAppList constructor(
  private val context: Context
) : IPlugin {

  fun getAllAppInfo(outputStream: OutputStream?) {
    val packages: List<PackageInfo> =
      context.packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES)

    for (i in packages.indices) {
      val packageInfo = packages[i]
      if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
        val tmpInfo = SonicAppInfo(
          appName = packageInfo.applicationInfo.loadLabel(context.packageManager).toString(),
          packageName = packageInfo.packageName,
          versionName = packageInfo.versionName,
          versionCode = packageInfo.versionCode,
          appIcon = Base64.encodeToString(
            ImageUtils.drawable2Bytes(
              packageInfo.applicationInfo.loadIcon(context.packageManager),
              Bitmap.CompressFormat.PNG,
              10
            ), Base64.NO_WRAP
          )
        )

        try {
          val dataBytes: ByteArray = GsonUtils.toJson(tmpInfo).toByteArray()
          // 数据长度转成二进制，存入byte[32]
          val lengthBytes = ByteArray(32)
          val binStr = Integer.toBinaryString(dataBytes.size).trim { it <= ' ' }
          val binArray = binStr.toCharArray()
          var x = binArray.size - 1
          var y = lengthBytes.size - 1
          while (x >= 0) {
            try {
              lengthBytes[y] = (binArray[x].toString() + "").toByte()
            } catch (e: Exception) {
              LogUtils.e(
                String.format(
                  "char transfer byte failed, char: %s",
                  binArray[x].toString() + ""
                )
              )
            }
            x--
            y--
          }
          // 先发送长度
          outputStream?.write(lengthBytes)
          outputStream?.flush()

          // 再发送数据
          outputStream?.write(dataBytes)
          outputStream?.flush()
        } catch (e: IOException) {
          e.printStackTrace()
        }
      }
    }
  }

  override fun initPlugin(context: Context) {

  }
}
