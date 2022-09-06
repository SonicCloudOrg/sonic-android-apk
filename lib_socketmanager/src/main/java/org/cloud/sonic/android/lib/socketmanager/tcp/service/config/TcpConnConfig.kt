package org.cloud.sonic.android.lib.socketmanager.tcp.service.config

import org.cloud.sonic.android.lib.socketmanager.tcp.helper.decode.AbsDecodeHelper
import org.cloud.sonic.android.lib.socketmanager.tcp.helper.decode.BaseDecodeHelper
import org.cloud.sonic.android.lib.socketmanager.tcp.helper.stickpackage.AbsStickPackageHelper
import org.cloud.sonic.android.lib.socketmanager.tcp.helper.stickpackage.BaseStickPackageHelper
import org.cloud.sonic.android.lib.socketmanager.tcp.helper.validation.AbsValidationHelper
import org.cloud.sonic.android.lib.socketmanager.tcp.helper.validation.BaseValidationHelper
import org.cloud.sonic.android.lib.socketmanager.utils.CharsetUtil
import org.cloud.sonic.android.lib.socketmanager.utils.StringValidationUtils
import java.nio.ByteOrder

/**
 * 连接配置
 */
class TcpConnConfig private constructor() {
    var charsetName: String = CharsetUtil.UTF_8 //默认编码
    var connTimeout: Int = 5000 //连接超时时间
    val receiveTimeout: Int = 0 //接受消息的超时时间,0为无限大
    var byteOrder = ByteOrder.BIG_ENDIAN //大端还是小端
    private var mStickPackageHelper: AbsStickPackageHelper = BaseStickPackageHelper() //解决粘包
    private var mDecodeHelper: AbsDecodeHelper = BaseDecodeHelper() //解析数据
    private var mValidationHelper: AbsValidationHelper = BaseValidationHelper() //消息验证
    var isReconnect = false //是否重连
    var localPort = -1
    val validationHelper: AbsValidationHelper
        get() = mValidationHelper
    val decodeHelper: AbsDecodeHelper
        get() = mDecodeHelper
    val stickPackageHelper: AbsStickPackageHelper
        get() = mStickPackageHelper

    class Builder {
        private val mTcpConnConfig: TcpConnConfig
        fun create(): TcpConnConfig {
            return mTcpConnConfig
        }

        fun setCharsetName(charsetName: String): Builder {
            mTcpConnConfig.charsetName = charsetName
            return this
        }

        fun setByteOrder(byteOrder: ByteOrder): Builder {
            mTcpConnConfig.byteOrder = byteOrder
            return this
        }

        fun setValidationHelper(validationHelper: AbsValidationHelper): Builder {
            mTcpConnConfig.mValidationHelper = validationHelper
            return this
        }

        fun setConnTimeout(timeout: Int): Builder {
            mTcpConnConfig.connTimeout = timeout
            return this
        }

        fun setIsReconnect(b: Boolean): Builder {
            mTcpConnConfig.isReconnect = b
            return this
        }

        //bug
        @Deprecated("")
        fun setLocalPort(localPort: Int): Builder {
            if (localPort > 0 && StringValidationUtils.validateRegex(
                    localPort.toString() + "",
                    StringValidationUtils.RegexPort
                )
            ) {
                mTcpConnConfig.localPort = localPort
            }
            return this
        }

        fun setStickPackageHelper(helper: AbsStickPackageHelper): Builder {
            mTcpConnConfig.mStickPackageHelper = helper
            return this
        }

        fun setDecodeHelper(helper: AbsDecodeHelper): Builder {
            mTcpConnConfig.mDecodeHelper = helper
            return this
        }

        init {
            mTcpConnConfig = TcpConnConfig()
        }
    }
}