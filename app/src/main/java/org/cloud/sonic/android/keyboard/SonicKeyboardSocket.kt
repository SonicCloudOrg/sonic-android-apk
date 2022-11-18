/*
 *  sonic-android-apk  Help your Android device to do more.
 *  Copyright (C) 2022 SonicCloudOrg
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.cloud.sonic.android.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import com.blankj.utilcode.util.LogUtils
import org.cloud.sonic.android.R
import org.cloud.sonic.android.lib.socketmanager.tcp.client.TcpClient
import org.cloud.sonic.android.lib.socketmanager.tcp.listener.TcpServerListener
import org.cloud.sonic.android.lib.socketmanager.tcp.model.TcpMassage
import org.cloud.sonic.android.lib.socketmanager.tcp.service.TcpServer
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpConnConfig
import org.cloud.sonic.android.lib.socketmanager.tcp.service.config.TcpServerConfig
import org.cloud.sonic.android.lib.socketmanager.utils.CharsetUtil

/**
 * sonic 输入法
 */
class SonicKeyboardSocket : InputMethodService(), TcpServerListener {

    companion object {
        private const val SONIC_KEYBOARD_SOCKET_PORT = "2335"
    }

    private var mSonicTcpServer: TcpServer? = null

    override fun onCreateInputView(): View {
        val mInputView: View = layoutInflater.inflate(R.layout.view, null)
        startSocket()
        return mInputView
    }

    override fun onDestroy() {
        super.onDestroy()
        closeSocket()
    }

    private fun startSocket() {
        if (mSonicTcpServer == null) {
            mSonicTcpServer =
                TcpServer.getTcpServer(SONIC_KEYBOARD_SOCKET_PORT.toInt())
            mSonicTcpServer?.let {
                it.addTcpServerListener(this)
                it.config(
                    TcpServerConfig.Builder()
                        .setTcpConnConfig(TcpConnConfig.Builder().create()).create()
                )
            }
        }
        mSonicTcpServer?.startServer()
    }

    private fun closeSocket() {
        mSonicTcpServer?.let {
            if (it.isListening()) {
                it.stopServer()
            }
        }
    }

    override fun onCreated(server: TcpServer) {
        LogUtils.d("服务启动成功")
    }

    override fun onListened(server: TcpServer) {
        LogUtils.d("服务 listenling ${server.getPort()}")
    }

    override fun onAccept(server: TcpServer, tcpClient: TcpClient) {
        LogUtils.d("收到客户端连接请求 ${tcpClient.getTargetInfo().ip}")
    }

    override fun onSent(server: TcpServer, tcpClient: TcpClient, tcpMsg: TcpMassage) {
        LogUtils.d("发送消息给 ${tcpClient.getTargetInfo().ip} 成功 msg= ${tcpMsg.getSourceDataString()}")
    }

    override fun onReceive(server: TcpServer, tcpClient: TcpClient, tcpMsg: TcpMassage) {
        //处理收到消息
        var msg = CharsetUtil.dataToString(
            tcpMsg.getSourceDataBytes(),
            CharsetUtil.UTF_8
        )

        var result = msg.split("CODE_AC_ENTER|CODE_AC_BACK|CODE_AC_CLEAN")

        for (r in result) {
            if (r == "CODE_AC_ENTER") {
                if (!sendDefaultEditorAction(true)) {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            } else if (r == "CODE_AC_BACK") {
                currentInputConnection.also { ic: InputConnection ->
                    ic.deleteSurroundingText(1, 0)
                }
            } else if (r == "CODE_AC_CLEAN") {
                currentInputConnection.also { ic: InputConnection ->
                    val curPos: CharSequence = ic.getExtractedText(ExtractedTextRequest(), 0).text
                    val beforePos: CharSequence? = ic.getTextBeforeCursor(curPos.length, 0)
                    val afterPos: CharSequence? = ic.getTextAfterCursor(curPos.length, 0)
                    if (beforePos != null && afterPos != null) {
                        ic.deleteSurroundingText(beforePos.length, afterPos.length)
                    }
                }
            } else {
                currentInputConnection.also { ic: InputConnection ->
                    ic.commitText(r, 1)
                }
            }
        }
    }

    override fun onValidationFail(server: TcpServer, client: TcpClient, tcpMsg: TcpMassage) {
    }

    override fun onClientClosed(
        server: TcpServer,
        tcpClient: TcpClient,
        msg: String,
        e: Exception?
    ) {
        LogUtils.d("客户端连接断开 ${tcpClient.getTargetInfo().ip}$msg$e")
    }

    override fun onServerClosed(server: TcpServer, msg: String?, e: Exception?) {
        LogUtils.d("服务器关闭 $server$msg$e")
    }

}