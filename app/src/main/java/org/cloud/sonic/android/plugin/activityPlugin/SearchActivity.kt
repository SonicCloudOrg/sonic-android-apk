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
package org.cloud.sonic.android.plugin.activityPlugin

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.reflect.InvocationTargetException

class SearchActivity : Activity() {

  @SuppressLint("MissingPermission")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val layout = LinearLayout(this)
    layout.keepScreenOn = true
    layout.orientation = LinearLayout.VERTICAL
    layout.layoutParams = LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.MATCH_PARENT
    )

    layout.setBackgroundColor(Color.parseColor("#409EFF"))
    layout.setPadding(16, 16, 16, 16)
    layout.gravity = Gravity.CENTER

    layout.addView(createTitleLabel())
    layout.addView(createLabel("MODEL"))
    layout.addView(createData(getProperty("ro.product.model", "unknown")))
    layout.addView(createLabel("MANUFACTURER"))
    layout.addView(createData(getProperty("ro.product.manufacturer", "unknown")))
    layout.addView(createLabel("SYSTEM VERSION"))
    layout.addView(createData("ANDROID " + Build.VERSION.RELEASE))
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    ensureVisibility()
    setContentView(layout)
  }


  private fun createLabel(text: String): View? {
    val titleView = TextView(this)
    titleView.gravity = Gravity.CENTER
    titleView.setTextColor(Color.parseColor("#606266"))
    titleView.textSize = 18f
    titleView.text = text
    return titleView
  }

  private fun createTitleLabel(): View? {
    val titleView = TextView(this)
    titleView.gravity = Gravity.CENTER
    titleView.setPadding(16, 16, 16, 40)
    titleView.setTextColor(Color.WHITE)
    titleView.textSize = 35f
    titleView.text = "I AM HERE"
    return titleView
  }

  private fun createData(text: String): View? {
    val dataView = TextView(this)
    dataView.gravity = Gravity.CENTER
    dataView.setTextColor(Color.WHITE)
    dataView.textSize = 24f
    dataView.text = text
    return dataView
  }

  private fun ensureVisibility() {
    val window = window
    window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    unlock()
    val params = window.attributes
    params.screenBrightness = 1.0f
    window.attributes = params
  }

  private fun getProperty(name: String, defaultValue: String): String {
    return try {
      val SystemProperties = Class.forName("android.os.SystemProperties")
      val get = SystemProperties.getMethod(
        "get",
        String::class.java,
        String::class.java
      )
      get.invoke(SystemProperties, name, defaultValue) as String
    } catch (e: ClassNotFoundException) {
      defaultValue
    } catch (e: NoSuchMethodException) {
      defaultValue
    } catch (e: InvocationTargetException) {
      defaultValue
    } catch (e: IllegalAccessException) {
      defaultValue
    }
  }

  private fun unlock() {
    val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
    keyguardManager.newKeyguardLock("InputService/Unlock").disableKeyguard()
  }

}
