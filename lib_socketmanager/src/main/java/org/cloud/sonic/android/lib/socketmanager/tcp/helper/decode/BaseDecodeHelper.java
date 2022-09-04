package org.cloud.sonic.android.lib.socketmanager.tcp.helper.decode;


import org.cloud.sonic.android.lib.socketmanager.tcp.model.TargetInfo;
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpConnConfig;

public class BaseDecodeHelper implements AbsDecodeHelper {
    @Override
    public byte[][] execute(byte[] data, TargetInfo targetInfo, TcpConnConfig tcpConnConfig) {
        return new byte[][]{data};
    }
}
