package org.cloud.sonic.android.model

data class SonicAppInfo(
    val appName:String,
    val packageName:String,
    val versionName:String,
    val versionCode:Int,
    val appIcon:ByteArray
)
