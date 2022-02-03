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
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.util.Log;


import org.cloud.sonic.android.recorder.mp3.Mp3EncodeThread;
import org.cloud.sonic.android.recorder.utils.ByteUtils;
import org.cloud.sonic.android.recorder.utils.FileUtils;
import org.cloud.sonic.android.recorder.utils.Logger;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

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

    private static final String SOCKET_NAME = "sonicaudioservice";


    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 2;

    private final Handler handler = new ConnectionHandler(this);
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private Thread recorderThread;
//    private long presentationTimeUs;
    // 处理 AAC 录制音频
    private MediaRecorder mediaRecorder;
    // 处理录制时候的数据流
    private ParcelFileDescriptor parcelWrite;
    private DataInputStream inputStream;

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
//        initTransferACC();
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

        if (isRunning()) {
            return START_NOT_STICKY;
        }

        Intent data = intent.getParcelableExtra(EXTRA_MEDIA_PROJECTION_DATA);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
        if (mediaProjection != null) {
            startRecording();
        } else {
            Log.w(TAG, "Failed to capture audio");
            stopSelf();
        }
        return START_NOT_STICKY;
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

    private static LocalSocket connect() throws IOException {
        LocalServerSocket localServerSocket = new LocalServerSocket(SOCKET_NAME);
        try {
            return localServerSocket.accept();
        } finally {
            localServerSocket.close();
        }
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

    private MediaRecorder createMediaRecorder(AudioRecord recorder){
        MediaRecorder mediaRecorder = new MediaRecorder();
        //配置采集方式，这里用的是麦克风的采集方式
        mediaRecorder.setAudioSource(recorder.getAudioSource());
        //配置输出方式，这里用的是AAC
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        //配置采样频率，频率越高月接近原始声音，Android所有设备都支持的采样频率为44100
        mediaRecorder.setAudioSamplingRate(44100);
        //配置文件的编码格式,AAC是比较通用的编码格式
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //配置码率，这里一般通用的是96000
        mediaRecorder.setAudioEncodingBitRate(96000);
        //设置获取音频流的方式
        mediaRecorder.setOutputFile(parcelWrite.getFileDescriptor());
        return mediaRecorder;
    }


    //record audio
    private void startRecording() {
        //处理线性输出
        intPipLine();
        final AudioRecord recorder = createAudioRecord(mediaProjection);
        mediaRecorder = createMediaRecorder(recorder);

        //try not thread
        recorderThread = new Thread(() -> {

            try (LocalSocket socket = connect()) {
                handler.sendEmptyMessage(MSG_CONNECTION_ESTABLISHED);
                mediaRecorder.prepare();
                mediaRecorder.start();
//                recorder.startRecording();
//                int BUFFER_MS = 15; // do not buffer more than BUFFER_MS milliseconds
                byte[] buffer = new byte[900];
//                short[] buf = new short[SAMPLE_RATE * CHANNELS * BUFFER_MS / 1000];
                while (true) {
//                    int r = recorder.read(buf, 0, buf.length);
                    int read = inputStream.read(buffer);
//                        encodePCMToAAC(buf,socket.getOutputStream());
                    if (read != -1) {
                        byte[] data = Arrays.copyOfRange(buffer, 0, read);
                        socket.getOutputStream().write(data, 0, read);
                    }
                }
            } catch (IOException e) {
                // ignore
            } finally {
//                recorder.stop();
                mediaRecorder.stop();
                stopSelf();
            }
        });
        recorderThread.start();
    }

    private boolean isRunning() {
        return recorderThread != null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (recorderThread != null) {
            recorderThread.interrupt();
            recorderThread = null;
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

    private void intPipLine() {
        try {
            ParcelFileDescriptor[] parcelFileDescriptors = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor parcelRead = new ParcelFileDescriptor(parcelFileDescriptors[0]);
            parcelWrite = new ParcelFileDescriptor(parcelFileDescriptors[1]);
            inputStream = new DataInputStream(new ParcelFileDescriptor.AutoCloseInputStream(parcelRead));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
