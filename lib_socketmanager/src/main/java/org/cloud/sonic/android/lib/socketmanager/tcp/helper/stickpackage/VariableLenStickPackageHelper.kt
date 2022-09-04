package org.cloud.sonic.android.lib.socketmanager.tcp.helper.stickpackage

import org.cloud.sonic.android.lib.socketmanager.utils.ExceptionUtils.throwException
import org.cloud.sonic.android.lib.socketmanager.tcp.helper.stickpackage.AbsStickPackageHelper
import org.cloud.sonic.android.lib.socketmanager.utils.ExceptionUtils
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import java.util.ArrayList
import kotlin.experimental.and

/**
 * 可变长度的粘包处理，使用于协议中有长度字段
 * 例：协议为:       type+dataLen+data+md5
 * type:命名类型，两个字节
 * dataLen:data字段的长度,两个字节
 * data:数据字段,长度不定，长度为dataLen
 * md5:md5字段，8个字节
 * 使用：      1.byteOrder:首先确定大小端，ByteOrder.BIG_ENDIAN or ByteOrder.LITTLE_ENDIAN;
 * 2.lenSize:len字段的长度，这个例子为2
 * 3.lenIndex：len字段的位置，这个例子为2，因为len字段前面为type，它长度为2
 * 4.offset：整个包的长度-len，这个例子是，type+dataLen+md5 三个字段的长度，也就是2+2+8=12
 *
 */
class VariableLenStickPackageHelper(
    byteOrder: ByteOrder,
    lenSize: Int,
    lenIndex: Int,
    offset: Int
) : AbsStickPackageHelper {
    private var offset = 0 //整体长度的偏移，比如有一个字段不算在len字段内
    private var lenIndex = 0 //长度字段的位置
    private var lenSize = 2 //len字段的长度，一般是short(2),int(4)
    private var byteOrder = ByteOrder.BIG_ENDIAN //大端还是小端
    private val mBytes: MutableList<Byte>
    private val lenStartIndex //len字段的开始位置
            : Int
    private val lenEndIndex //len字段的结束位置，[lenStartIndex,lenEndIndex]
            : Int

    private fun getLen(src: ByteArray, order: ByteOrder): Int {
        var re = 0
        if (order == ByteOrder.BIG_ENDIAN) {
            for (b in src) {
                re = re shl 8 or ((b and 0xff.toByte()).toInt())
            }
        } else {
            for (i in src.indices.reversed()) {
                re = re shl 8 or ((src[i] and 0xff.toByte()).toInt())
            }
        }
        return re
    }

    override fun execute(`is`: InputStream?): ByteArray? {
        mBytes.clear()
        var count = 0
        var len = -1
        var temp: Byte
        val result: ByteArray
        var msgLen = -1
        val lenField = ByteArray(lenSize)
        try {
            while (`is`!!.read().also { len = it } != -1) {
                temp = len.toByte()
                if (count in lenStartIndex..lenEndIndex) {
                    lenField[count - lenStartIndex] = temp //保存len字段
                    if (count == lenEndIndex) { //len字段保存结束，需要解析出来具体的长度了
                        msgLen = getLen(lenField, byteOrder)
                    }
                }
                count++
                mBytes.add(temp)
                if (msgLen != -1) { //已结解析出来长度
                    if (count == msgLen + offset) {
                        break
                    } else if (count > msgLen + offset) { //error
                        len = -1 //标记为error
                        break
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
        result = ByteArray(mBytes.size)
        for (i in result.indices) {
            result[i] = mBytes[i]
        }
        return result
    }

    init {
        this.byteOrder = byteOrder
        this.lenSize = lenSize
        this.offset = offset
        this.lenIndex = lenIndex
        mBytes = ArrayList()
        lenStartIndex = lenIndex
        lenEndIndex = lenIndex + lenSize - 1
        if (lenStartIndex > lenEndIndex) {
            throwException("lenStartIndex>lenEndIndex")
        }
    }
}