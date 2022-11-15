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
package org.cloud.sonic.android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.AppUtils
import com.gyf.immersionbar.ktx.immersionBar
import org.cloud.sonic.android.databinding.ActivityMainBinding
import org.cloud.sonic.android.service.SonicManagerServiceV2

class SonicServiceActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    immersionBar {
      statusBarColor(R.color.auto_bg)
      navigationBarColor(R.color.auto_bg)
      statusBarDarkFont(true)
      autoDarkModeEnable(true)
    }

    SonicManagerServiceV2.start(this)

    Handler(Looper.getMainLooper()) {
      finish()
      false
    }.sendEmptyMessageDelayed(0, 1500)
    binding.version.text = AppUtils.getAppVersionName()
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
  }
}
