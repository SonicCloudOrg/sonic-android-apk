package org.cloud.sonic.android.lib.socketmanager.tcp.helper.decode;


import org.cloud.sonic.android.lib.socketmanager.tcp.model.TargetInfo;
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpConnConfig;

/**
 * 解析消息的处理
 */
public interface AbsDecodeHelper {
    /**
     *
     * @param data  完整的数据包
     * @param targetInfo    对方的信息(ip/port)
     * @param tcpConnConfig    tcp连接配置，可自定义
     * @return
     */
    byte[][] execute(byte[] data, TargetInfo targetInfo, TcpConnConfig tcpConnConfig);
}
