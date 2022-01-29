package org.cloud.sonic.android.models;

import org.cloud.sonic.android.util.ByteUtils;

public class WavHeader {
    /**
     * RIFF数据块
     */
    final String riffChunkId = "RIFF";
    int riffChunkSize;
    final String riffType = "WAVE";

    /**
     * FORMAT 数据块
     */
    final String formatChunkId = "fmt ";
    final int formatChunkSize = 16;
    final short audioFormat = 1;
    short channels;
    int sampleRate;
    int byteRate;
    short blockAlign;
    short sampleBits;

    /**
     * FORMAT 数据块
     */
    final String dataChunkId = "data";
    int dataChunkSize;

    WavHeader(int totalAudioLen, int sampleRate, short channels, short sampleBits) {
        this.riffChunkSize = totalAudioLen;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.byteRate = sampleRate * sampleBits / 8 * channels;
        this.blockAlign = (short) (channels * sampleBits / 8);
        this.sampleBits = sampleBits;
        this.dataChunkSize = totalAudioLen - 44;
    }

    public byte[] getHeader() {
        byte[] result;
        result = ByteUtils.merger(ByteUtils.toBytes(riffChunkId), ByteUtils.toBytes(riffChunkSize));
        result = ByteUtils.merger(result, ByteUtils.toBytes(riffType));
        result = ByteUtils.merger(result, ByteUtils.toBytes(formatChunkId));
        result = ByteUtils.merger(result, ByteUtils.toBytes(formatChunkSize));
        result = ByteUtils.merger(result, ByteUtils.toBytes(audioFormat));
        result = ByteUtils.merger(result, ByteUtils.toBytes(channels));
        result = ByteUtils.merger(result, ByteUtils.toBytes(sampleRate));
        result = ByteUtils.merger(result, ByteUtils.toBytes(byteRate));
        result = ByteUtils.merger(result, ByteUtils.toBytes(blockAlign));
        result = ByteUtils.merger(result, ByteUtils.toBytes(sampleBits));
        result = ByteUtils.merger(result, ByteUtils.toBytes(dataChunkId));
        result = ByteUtils.merger(result, ByteUtils.toBytes(dataChunkSize));
        return result;
    }
}
