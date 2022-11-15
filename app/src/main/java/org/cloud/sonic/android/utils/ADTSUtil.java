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
package org.cloud.sonic.android.utils;

public class ADTSUtil {
    /** 只保存了ADTS头中不变化的那部分信息 */
    private static long adtsPart = 0xFFF94000001FFCL;
    /** 保存了ADTS头中的完整信息 */
    private static long adtsFull;
    private static byte byte_0;
    private static byte byte_1;
    private static byte byte_2;
    private static byte byte_6;

    /**
     * 初始化ADTS头，计算出ADTS头中固定不变化的那些信息
     * @param samplingFrequencyIndex 音频采样频率的索引
     * @param channelCount 声道数量
     */
    public static void initADTS(long samplingFrequencyIndex, long channelCount) {
        adtsPart = adtsPart | (samplingFrequencyIndex << 34) | (channelCount << 30);
        byte_0 = (byte) (adtsPart >>> 48); // 右移48 = （7字节 - 1） * 8
        byte_1 = (byte) (adtsPart >>> 40); // 右移40 = （6字节 - 1） * 8
        byte_2 = (byte) (adtsPart >>> 32); // 右移32 = （5字节 - 1） * 8
        byte_6 = (byte) adtsPart;          // 右移 0 = （1字节 - 1） * 8
    }

    /** 往oneADTSFrameBytes数组中的最前面7个字节中填入ADTS头信息 */
    public static void addADTS(byte[] oneADTSFrameBytes) {
        adtsFull = adtsPart | (oneADTSFrameBytes.length << 13);// 一个int32位，所以不用担心左移13位数据丢失的问题
        oneADTSFrameBytes[0] = byte_0;
        oneADTSFrameBytes[1] = byte_1;
        oneADTSFrameBytes[2] = byte_2;
        oneADTSFrameBytes[3] = (byte) (adtsFull >>> 24); // 右移24 = （4字节 - 1） * 8
        oneADTSFrameBytes[4] = (byte) (adtsFull >>> 16); // 右移16 = （3字节 - 1） * 8
        oneADTSFrameBytes[5] = (byte) (adtsFull >>> 8);  // 右移 8 = （2字节 - 1） * 8
        oneADTSFrameBytes[6] = byte_6;
    }
}
