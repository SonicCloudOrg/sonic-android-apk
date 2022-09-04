package org.cloud.sonic.android.lib.socketmanager.tcp.helper.stickpackage

import org.cloud.sonic.android.lib.socketmanager.tcp.helper.stickpackage.AbsStickPackageHelper
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * 最简单的做法，不处理粘包，直接读取返回，最大长度256
 */
class BaseStickPackageHelper : AbsStickPackageHelper {
    private var maxLen = 256 //最大长度256

    constructor() {}
    constructor(maxLen: Int) {
        if (maxLen > 0) {
            this.maxLen = maxLen
        }
    }

    override fun execute(`is`: InputStream?): ByteArray? {
        val bytes = ByteArray(maxLen)
        var len: Int
        try {
            if (`is`!!.read(bytes).also { len = it } != -1) {
                return Arrays.copyOf(bytes, len)
            }
        } catch (e: IOException) {
//            e.printStackTrace();
        }
        return null
    }
}