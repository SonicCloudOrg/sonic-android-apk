package org.cloud.sonic.android

import android.app.Application
import com.blankj.utilcode.util.Utils

//@HiltAndroidApp
class App:Application() {
    override fun onCreate() {
        super.onCreate()
        //初始化工具类
        Utils.init(this)
    }
}