/*
 *  Copyright (C) [SonicCloudOrg] Sonic Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.cloud.sonic.android.lib.socketmanager.tcp.model

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class TcpMassage {
    enum class MsgType {
        Send, Receive
    }

    private val IDAtomic = AtomicInteger()
    private var id = 0
    private var sourceDataBytes: ByteArray? = null // 数据源
    private var sourceDataString: String? = null // 数据源
    private var target: TargetInfo? = null
    private var time: Long = 0 // 发送、接受消息的时间戳
    private var mMsgType = MsgType.Send
    private var endDecodeData: Array<ByteArray>? = null

    constructor (id:Int) {
        this.id = id
    }

    constructor(data: ByteArray, target: TargetInfo?, type: MsgType) {
        sourceDataBytes = data
        this.target = target
        mMsgType = type
        init()
    }

    constructor(data: String?, target: TargetInfo?, type: MsgType) {
        this.target = target
        sourceDataString = data
        mMsgType = type
        init()
    }

    fun setTime() {
        time = System.currentTimeMillis()
    }

    private fun init() {
        id = IDAtomic.getAndIncrement()
    }

    fun getTime(): Long {
        return time
    }

    fun getEndDecodeData(): Array<ByteArray>? {
        return endDecodeData
    }

    fun setEndDecodeData(endDecodeData: Array<ByteArray>) {
        this.endDecodeData = endDecodeData
    }

    fun getMsgType(): MsgType {
        return mMsgType
    }

    fun setMsgType(msgType: MsgType) {
        mMsgType = msgType
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val tcpMsg: TcpMassage = o as TcpMassage
        return id == tcpMsg.id
    }

    override fun toString(): String {
        val sb = StringBuffer()
        if (endDecodeData != null) {
            for (bs in endDecodeData!!) {
                sb.append(Arrays.toString(bs))
            }
        }
        return "TcpMsg{" +
                "sourceDataBytes=" + Arrays.toString(sourceDataBytes) +
                ", id=" + id +
                ", sourceDataString='" + sourceDataString + '\'' +
                ", target=" + target +
                ", time=" + time +
                ", msgtyoe=" + mMsgType +
                ", enddecode=" + sb.toString() +
                '}'
    }

    override fun hashCode(): Int {
        return id
    }


    fun getSourceDataBytes(): ByteArray? {
        return sourceDataBytes
    }

    fun setSourceDataBytes(sourceDataBytes: ByteArray) {
        this.sourceDataBytes = sourceDataBytes
    }

    fun getSourceDataString(): String? {
        return sourceDataString
    }

    fun setSourceDataString(sourceDataString: String?) {
        this.sourceDataString = sourceDataString
    }

    fun getId(): Int {
        return id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getIDAtomic(): AtomicInteger {
        return IDAtomic
    }

    fun getTarget(): TargetInfo? {
        return target
    }

    fun setTarget(target: TargetInfo?) {
        this.target = target
    }
}
