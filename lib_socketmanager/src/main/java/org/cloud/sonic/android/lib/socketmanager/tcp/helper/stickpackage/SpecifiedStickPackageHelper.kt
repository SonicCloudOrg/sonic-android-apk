package org.cloud.sonic.android.lib.socketmanager.tcp.helper.stickpackage

import org.cloud.sonic.android.lib.socketmanager.utils.ExceptionUtils
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * 特定字符的粘包处理,首尾各一个Byte[],不可以同时为空，如果其中一个为空，那么以不为空的作为分割标记
 * 例：协议制定为  ^+数据+$，首就是^，尾是$
 */
class SpecifiedStickPackageHelper(private val head: ByteArray?, private val tail: ByteArray?) :
    AbsStickPackageHelper {
    private val bytes: MutableList<Byte>
    private val headLen: Int
    private val tailLen: Int
    private fun endWith(src: ByteArray, target: ByteArray?): Boolean {
        if (src.size < target!!.size) {
            return false
        }
        for (i in target.indices) { //逆序比较
            if (target[target.size - i - 1] != src[src.size - i - 1]) {
                return false
            }
        }
        return true
    }

    private fun getRangeBytes(list: List<Byte>, start: Int, end: Int): ByteArray {
        val temps = list.toTypedArray().copyOfRange(start, end)
        val result = ByteArray(temps.size)
        for (i in result.indices) {
            result[i] = temps[i]
        }
        return result
    }

    override fun execute(`is`: InputStream?): ByteArray? {
        bytes.clear()
        var len = -1
        var temp: Byte
        var startIndex = -1
        var result: ByteArray? = null
        var isFindStart = false
        var isFindEnd = false
        try {
            while (`is`!!.read().also { len = it } != -1) {
                temp = len.toByte()
                bytes.add(temp)
                val byteArray = bytes.toByteArray()
                if (headLen == 0 || tailLen == 0) { //只有头或尾标记
                    if (endWith(byteArray, head) || endWith(byteArray, tail)) {
                        if (startIndex == -1) {
                            startIndex = bytes.size - headLen
                        } else { //找到了
                            result = getRangeBytes(bytes, startIndex, bytes.size)
                            break
                        }
                    }
                } else {
                    if (!isFindStart) {
                        if (endWith(byteArray, head)) {
                            startIndex = bytes.size - headLen
                            isFindStart = true
                        }
                    } else if (!isFindEnd) {
                        if (endWith(byteArray, tail)) {
                            if (startIndex + headLen <= bytes.size - tailLen) {
                                isFindEnd = true
                                result = getRangeBytes(bytes, startIndex, bytes.size)
                                break
                            }
                        }
                    }
                }
            }
            if (len == -1) {
                return null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return result
    }

    init {
        if (head == null || tail == null) {
            ExceptionUtils.throwException(" head or tail ==null")
        }
        if (head!!.isEmpty() && tail!!.size == 0) {
            ExceptionUtils.throwException(" head and tail length==0")
        }
        headLen = head.size
        tailLen = tail!!.size
        bytes = ArrayList()
    }
}