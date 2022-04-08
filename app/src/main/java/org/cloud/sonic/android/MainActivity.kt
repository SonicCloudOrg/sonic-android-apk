package org.cloud.sonic.android

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ktx.immersionBar
import org.cloud.sonic.android.service.SonicManagerService

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_START_CAPTURE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        immersionBar {
            statusBarColor(R.color.white)
            navigationBarColor(R.color.white)
            statusBarDarkFont(true)
        }

        SonicManagerService.start(this)

        Handler(Looper.getMainLooper()) {
            finish()
            false
        }.sendEmptyMessageDelayed(0,1500)


    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}