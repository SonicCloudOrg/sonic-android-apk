package org.cloud.sonic.android.util;

import android.os.FileUtils;
import android.util.Log;

import org.cloud.sonic.android.models.WavHeader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * 生成头字节
 */
public class WavUtils {
    private static final String TAG = WavUtils.class.getSimpleName();

    /**
     * 生成wav格式的Header
     * wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
     * FMT Chunk，Fact chunk（可选）,Data chunk
     *
     * @param totalAudioLen 不包括header的音频数据总长度
     * @param sampleRate    采样率,也就是录制时使用的频率
     * @param channels      audioRecord的频道数量
     * @param sampleBits    位宽
     */
    public static byte[] generateWavFileHeader(int totalAudioLen, int sampleRate, int channels, int sampleBits) {
        WavHeader wavHeader = new WavHeader(totalAudioLen, sampleRate, (short) channels, (short) sampleBits);
        return wavHeader.getHeader();
    }

    /**
     * 将header写入到pcm文件中 不修改文件名
     *
     * @param file   写入的pcm文件
     * @param header wav头数据
     */
    public static void writeHeader(File file, byte[] header) {
        if (!file.exists()) {
            return;
        }

        RandomAccessFile wavRaf = null;
        try {
            wavRaf = new RandomAccessFile(file, "rw");
            wavRaf.seek(0);
            wavRaf.write(header);
            wavRaf.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            try {
                if (wavRaf != null) {
                    wavRaf.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
