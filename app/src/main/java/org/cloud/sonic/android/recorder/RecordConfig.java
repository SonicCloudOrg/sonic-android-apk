package org.cloud.sonic.android.recorder;

import android.media.AudioFormat;
import android.os.Environment;

import java.io.Serializable;
import java.util.Locale;

/**
 * @author jeff on 2020/8/19.
 */
public class RecordConfig implements Serializable {
    /**
     * 录音格式 默认WAV格式
     */
    private RecordFormat format = RecordFormat.MP3;
    /**
     * 通道数:默认单通道
     */
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 位宽
     */
    private int encodingConfig = AudioFormat.ENCODING_PCM_16BIT;
    /**
     * 采样率
     */
    private int sampleRate = 44100;
    /**
     * 是否开启增益效果:默认开启
     */
    private boolean isProcessNose = true;
    /**
     * 录音文件存放路径，默认sdcard/Record
     */
    private String recordDir = String.format(Locale.getDefault(),
            "%s/Record/",
            Environment.getExternalStorageDirectory().getAbsolutePath());

    public RecordConfig(){}

    public RecordConfig(Builder builder) {
        this.format = builder.format;
        this.channelConfig = builder.channelConfig;
        this.encodingConfig = builder.encodingConfig;
        this.sampleRate = builder.sampleRate;
        this.recordDir = builder.recordDir;
    }

    /**
     * 获取是否开启增益，默认开启
     * @return 是否开启
     */

    public boolean isProcessNose() {
        return isProcessNose;
    }

    public String getRecordDir() {
        return recordDir;
    }

    /**
     * 获取当前录音的采样位宽 单位bit
     *
     * @return 采样位宽 0: error
     */
    public int getEncoding() {
        if(format == RecordFormat.MP3){//mp3后期转换
            return 16;
        }

        if (encodingConfig == AudioFormat.ENCODING_PCM_8BIT) {
            return 8;
        } else if (encodingConfig == AudioFormat.ENCODING_PCM_16BIT) {
            return 16;
        } else {
            return 0;
        }
    }

    public void setFormat(RecordFormat format) {
        this.format = format;
    }

    public void setChannelConfig(int channelConfig) {
        this.channelConfig = channelConfig;
    }

    public RecordConfig setEncodingConfig(int encodingConfig) {
        this.encodingConfig = encodingConfig;
        return this;
    }

    public RecordConfig setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    public void setProcessNose(boolean processNose) {
        isProcessNose = processNose;
    }

    public void setRecordDir(String recordDir) {
        this.recordDir = recordDir;
    }

    /**
     * 获取当前录音的采样位宽 单位bit
     *
     * @return 采样位宽 0: error
     */
    public int getRealEncoding() {
        if (encodingConfig == AudioFormat.ENCODING_PCM_8BIT) {
            return 8;
        } else if (encodingConfig == AudioFormat.ENCODING_PCM_16BIT) {
            return 16;
        } else {
            return 0;
        }
    }

    /**
     * 当前的声道数
     *
     * @return 声道数： 0：error
     */
    public int getChannelCount() {
        if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
            return 1;
        } else if (channelConfig == AudioFormat.CHANNEL_IN_STEREO) {
            return 2;
        } else {
            return 0;
        }
    }

    public RecordFormat getFormat() {
        return format;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public int getEncodingConfig() {
        if(format == RecordFormat.MP3){//mp3后期转换
            return AudioFormat.ENCODING_PCM_16BIT;
        }
        return encodingConfig;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "录制格式： %s,采样率：%sHz,位宽：%s bit,声道数：%s", format, sampleRate, getEncoding(), getChannelCount());
    }

    public enum RecordFormat {
        /**
         * mp3格式
         */
        MP3(".mp3"),
        /**
         * wav格式
         */
        WAV(".wav"),
        /**
         * pcm格式
         */
        PCM(".pcm");

        private String extension;

        public String getExtension() {
            return extension;
        }

        RecordFormat(String extension) {
            this.extension = extension;
        }
    }

    public static class Builder {
        /**
         * 录音格式 默认WAV格式
         */
        private RecordFormat format = RecordFormat.MP3;
        /**
         * 通道数:默认双通道
         */
        private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        /**
         * 位宽
         */
        private int encodingConfig = AudioFormat.ENCODING_PCM_16BIT;
        /**
         * 采样率
         */
        private int sampleRate = 44100;
        /**
         * 是否开启增益效果:默认开启
         */
        private boolean isProcessNose = false;
        /**
         * 录音文件存放路径，默认sdcard/Record
         */
        private String recordDir = String.format(Locale.getDefault(),
                "%s/Record/",
                Environment.getExternalStorageDirectory().getAbsolutePath());

        public RecordConfig build(){
            // 校验逻辑放到这里来做，包括必填项校验、依赖关系校验、约束条件校验等
            return new RecordConfig(this);
        }

        public Builder setFormat(RecordFormat format) {
            this.format = format;
            return this;
        }

        public Builder setChannelConfig(int channelConfig) {
            this.channelConfig = channelConfig;
            return this;
        }

        public Builder setEncodingConfig(int encodingConfig ){
            this.encodingConfig = encodingConfig;
            return this;
        }

        public Builder setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder setProcessNose(boolean processNose) {
            isProcessNose = processNose;
            return this;
        }

        public Builder setRecordDir(String recordDir) {
            this.recordDir = recordDir;
            return this;
        }
    }
}
