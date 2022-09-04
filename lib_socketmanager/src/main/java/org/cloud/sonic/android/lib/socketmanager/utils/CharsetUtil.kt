package org.cloud.sonic.android.lib.socketmanager.utils

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

object CharsetUtil {
    const val UTF_8 = "UTF-8"
    const val GBK = "GBK"

    fun stringToData(string: String?, charsetName: String?): ByteArray? {
        if (string != null) {
            try {
                return string.toByteArray(charset(charsetName!!))
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun dataToString(data: ByteArray?, charsetName: String?): String {
        data?.let {
            try {
                return String(data, Charset.forName(charsetName))
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        return ""
    }
}