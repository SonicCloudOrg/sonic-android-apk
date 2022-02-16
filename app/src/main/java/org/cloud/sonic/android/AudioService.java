package org.cloud.sonic.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;


import org.cloud.sonic.android.recorder.utils.Logger;
import org.cloud.sonic.android.util.ADTSUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Eason, sndcpy
 * More https://github.com/rom1v/sndcpy
 */
public class AudioService extends Service {

    private static final String TAG = "sonicaudioservice";
    private static final String CHANNEL_ID = "sonicaudioservice";
    private static final int NOTIFICATION_ID = 1;

    private static final String ACTION_RECORD = "org.cloud.sonic.android.RECORD";
    private static final String ACTION_STOP = "org.cloud.sonic.android.STOP";
    private static final String EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData";

    private static final int MSG_CONNECTION_ESTABLISHED = 1;


    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = 2;

    private final Handler handler = new ConnectionHandler(this);
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    // 处理 AAC 录制音频
    private MediaCodec mMediaCodec;
    private AudioRecord mAudioRecord;

    private Thread workThread;
    //开启 LocalServiceSocket 的服务器
    private LocalServerSocket serverSocket;
    private LocalSocket clientSocket;
    private OutputStream outputStream;
    private InputStream mInputStream;

    //超时时间 30s
    static final int LINK_SOCKET_TIMEOUT = 30*1000;
    //定义在ActivityManagerService中，Service超时消息的what值
    static final int LINK_SOCKET_TIMEOUT_MSG = 0;
    static final int REC_SERVICE_ACTION = 1;

    private int mBufferSize = 4 * 1024;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what){
                case LINK_SOCKET_TIMEOUT_MSG:{
                    stopSelf();
                    break;
                }
                case REC_SERVICE_ACTION:{
                    String recMes = (String) msg.obj;
                    if (ACTION_STOP.equals(recMes)) {
                        stopSelf();
                    }
                    break;
                }
                default:{
                    Log.e("AudioService","why are you here?");
                }
            }

        }
    };


    @SuppressLint("NewApi")
    public static void start(Context context, Intent data) {
        Intent intent = new Intent(context, AudioService.class);
        intent.setAction(ACTION_RECORD);
        intent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data);
        context.startForegroundService(intent);
    }

    @Override
    @SuppressLint("NewApi")
    public void onCreate() {
        super.onCreate();
        Notification notification = createNotification(false);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_NONE);
        getNotificationManager().createNotificationChannel(channel);

        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
    }

    @Override
    @SuppressLint("NewApi")
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            //exit
            stopSelf();
            return START_NOT_STICKY;
        }

        Intent data = intent.getParcelableExtra(EXTRA_MEDIA_PROJECTION_DATA);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);

        if (mediaProjection != null) {
            startRecording();
            //必须要在子线程里接收消息
            new Thread(this::acceptMsg).start();
        } else {
            Log.w(TAG, "Failed to capture audio");
            stopSelf();
        }
        linkTimeOutStop();
        return START_NOT_STICKY;
    }

    private void linkTimeOutStop() {
        Message msg = mHandler.obtainMessage(
            LINK_SOCKET_TIMEOUT_MSG);
        msg.obj = "LINK_SOCKET_TIMEOUT";
        //当超时后仍没有remove该SERVICE_TIMEOUT_MSG消息，则执行service Timeout流程【见2.3.1】
        mHandler.sendMessageDelayed(msg, LINK_SOCKET_TIMEOUT);
        //在这里，把加入的延时消息给移除掉了
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("NewApi")
    private Notification createNotification(boolean established) {
        Notification.Builder notificationBuilder = new Notification.Builder(this, CHANNEL_ID);
        notificationBuilder.setContentTitle(getString(R.string.app_name));
        int textRes = established ? R.string.notification_forwarding : R.string.notification_waiting;
        notificationBuilder.setContentText(getText(textRes));
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.addAction(createStopAction());
        return notificationBuilder.build();
    }


    private Intent createStopIntent() {
        Intent intent = new Intent(this, AudioService.class);
        intent.setAction(ACTION_STOP);
        return intent;
    }

    @SuppressLint("NewApi")
    private Notification.Action createStopAction() {
        Intent stopIntent = createStopIntent();
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        Icon stopIcon = Icon.createWithResource(this, R.drawable.ic_launcher);
        String stopString = getString(R.string.action_stop);
        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(stopIcon, stopString, stopPendingIntent);
        return actionBuilder.build();
    }


    @SuppressLint("NewApi")
    private static AudioPlaybackCaptureConfiguration createAudioPlaybackCaptureConfig(MediaProjection mediaProjection) {
        AudioPlaybackCaptureConfiguration.Builder confBuilder = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection);
        confBuilder.addMatchingUsage(AudioAttributes.USAGE_MEDIA);
        confBuilder.addMatchingUsage(AudioAttributes.USAGE_GAME);
        confBuilder.addMatchingUsage(AudioAttributes.USAGE_UNKNOWN);
        return confBuilder.build();
    }

    @SuppressLint("NewApi")
    private static AudioFormat createAudioFormat() {
        AudioFormat.Builder builder = new AudioFormat.Builder();
        //raw pcm 16bit
        builder.setEncoding(AudioFormat.ENCODING_PCM_16BIT);
        builder.setSampleRate(SAMPLE_RATE);
        builder.setChannelMask(CHANNELS == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO);
        return builder.build();
    }

    @SuppressLint({"NewApi", "MissingPermission"})
    private static AudioRecord createAudioRecord(MediaProjection mediaProjection) {
        AudioRecord.Builder builder = new AudioRecord.Builder();
        builder.setAudioFormat(createAudioFormat());
        builder.setBufferSizeInBytes(1024 * 1024);
        builder.setAudioPlaybackCaptureConfig(createAudioPlaybackCaptureConfig(mediaProjection));
        return builder.build();
    }

    //record audio
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startRecording() {
        workThread =  new Thread("publish-thread") {
            @Override
            public void run() {
                try{
                    Log.i(TAG, String.format("creating socket %s", CHANNEL_ID));
                    serverSocket = new LocalServerSocket(CHANNEL_ID);
                    Log.i(TAG, String.format("Listening on %s", CHANNEL_ID));
                    clientSocket = serverSocket.accept();
                    Log.d(TAG, "client connected");
                    outputStream = clientSocket.getOutputStream();
                    //将之前埋的 30 秒炸弹关闭
                    mHandler.removeMessages(LINK_SOCKET_TIMEOUT_MSG);
                }catch (IOException e){
                    e.printStackTrace();

                }
            }
        };
        workThread.start();

        mAudioRecord = createAudioRecord(mediaProjection);
        mAudioRecord.startRecording();

        recordInternalAudio(mAudioRecord);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void recordInternalAudio(AudioRecord audioRecord) {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 196000);
            mediaFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
            ADTSUtil.initADTS(SAMPLE_RATE, CHANNELS);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            final int[] totalBytesRead = {0};
            final Long[] mPresentationTime = {0L};

            mMediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
                    ByteBuffer codecInputBuffer = mediaCodec.getInputBuffer(i);
                    int capacity = codecInputBuffer.capacity();
                    byte[] buffer = new byte[capacity];
                    int readBytes = audioRecord.read(buffer, 0, buffer.length);
                    if (readBytes > 0) {
                        codecInputBuffer.put(buffer, 0, readBytes);
                        mediaCodec.queueInputBuffer(i, 0, readBytes, mPresentationTime[0], 0);
                        totalBytesRead[0] += readBytes;
                        mPresentationTime[0] = 1000000L * (totalBytesRead[0] / 2) / 44100;
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferIndex, @NonNull MediaCodec.BufferInfo mBufferInfo) {
                    if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        Logger.i("AudioService", "AAC的配置数据");
                    } else {
                        byte[] oneADTSFrameBytes = new byte[7 + mBufferInfo.size];
                        ADTSUtil.addADTS(oneADTSFrameBytes);
                        ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
                        outputBuffer.get(oneADTSFrameBytes, 7, mBufferInfo.size);
                        if (outputStream!=null){
                            try {
                                outputStream.write(oneADTSFrameBytes,0,oneADTSFrameBytes.length);
                            } catch (IOException e) {
                                stopSelf();
                                e.printStackTrace();
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false);
                }

                @Override
                public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                    e.printStackTrace();
                    stopSelf();
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {

                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.start();
    }

    private boolean isRunning() {
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaProjection.stop();
        mAudioRecord.release();
        mMediaCodec.stop();
        mMediaCodec.release();
        disSocketService();
        stopForeground(true);
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void acceptMsg() {
        while (true) {
            if (clientSocket!=null){
                try {
                    byte[] buffer = new byte[1024];
                    mInputStream = clientSocket.getInputStream();
                    int count = mInputStream.read(buffer);
                    String key = new String(Arrays.copyOfRange(buffer, 0, count));
                    Log.d(TAG, "ServerActivity mSocketOutStream==" + key);
                    Message msg = mHandler.obtainMessage(REC_SERVICE_ACTION);
                    msg.obj = key;
                    msg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "exception==" + e.fillInStackTrace().getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void disSocketService(){
        try {
            if(serverSocket!=null){
                serverSocket.close();
                serverSocket = null;
            }
            if (clientSocket!=null){
                clientSocket.getOutputStream().close();
                clientSocket.close();
                clientSocket = null;
            }
            outputStream = null;
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static final class ConnectionHandler extends Handler {

        private AudioService service;

        ConnectionHandler(AudioService service) {
            this.service = service;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(Message message) {
            if (!service.isRunning()) {
                return;
            }

            if (message.what == MSG_CONNECTION_ESTABLISHED) {
                Notification notification = service.createNotification(true);
                service.getNotificationManager().notify(NOTIFICATION_ID, notification);
            }
        }
    }

}
