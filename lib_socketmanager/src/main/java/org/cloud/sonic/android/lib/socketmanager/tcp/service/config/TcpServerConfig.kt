package org.cloud.sonic.android.lib.socketmanager.tcp.service.config


/**
 * server配置
 */
class TcpServerConfig private constructor() {
    var maxClientSize = Int.MAX_VALUE
    var mTcpConnConfig: TcpConnConfig

    class Builder {
        private val tcpServerConfig: TcpServerConfig = TcpServerConfig()
        fun create(): TcpServerConfig {
            return tcpServerConfig
        }

        fun setMaxClientSize(maxSize: Int): Builder {
            tcpServerConfig.maxClientSize = maxSize
            return this
        }

        fun setTcpConnConfig(tcpConnConfig: TcpConnConfig): Builder {
            tcpServerConfig.mTcpConnConfig = tcpConnConfig
            return this
        }

    }

    init {
        mTcpConnConfig = TcpConnConfig.Builder().create()
    }
}