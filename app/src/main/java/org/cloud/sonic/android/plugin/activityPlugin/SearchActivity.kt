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

  private interface SecuredGetter<T> {
    fun get(): T
  }

  @SuppressLint("MissingPermission")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val intent = intent
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

    var serial = intent.getStringExtra("serial")

    if (serial == null) {
      serial = getProperty("ro.serialno", "unknown")
    }

    layout.addView(createTitleLabel())
    layout.addView(createLabel("SERIAL"))
    layout.addView(createData(serial!!))
    layout.addView(createLabel("MODEL"))
    layout.addView(createData(getProperty("ro.product.model", "unknown")))
    layout.addView(createLabel("VERSION"))
    layout.addView(createData(Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"))
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    ensureVisibility()
    setContentView(layout)
  }


  private fun createLabel(text: String): View? {
    val titleView = TextView(this)
    titleView.gravity = Gravity.CENTER
    titleView.setTextColor(Color.parseColor("#000000"))
    titleView.textSize = 16f
    titleView.text = text
    return titleView
  }

  private fun createTitleLabel(): View? {
    val titleView = TextView(this)
    titleView.gravity = Gravity.CENTER
    titleView.setPadding(16, 16, 16, 40)
    titleView.setTextColor(Color.WHITE)
    titleView.textSize = 35f
    titleView.text = "我在这里\nI am here!"
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
