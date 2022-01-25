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
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

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

    private MediaCodec mediaCodec;

    private void initTransferACC(){
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 16000, 1);//参数对应-> mime type、采样率、声道数
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 100);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16*1024);
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaCodec.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    @SuppressLint("NewApi")
    private void encodePCMToAAC(byte[] bytes) throws IOException {
        int inputBufIndex = mediaCodec.dequeueInputBuffer(1000);
        if (inputBufIndex >= 0) {
            ByteBuffer mInputBuffer = mediaCodec.getInputBuffer(inputBufIndex);
            mInputBuffer.clear();
            mInputBuffer.put(bytes, 0, 1024 * 1024);
            mediaCodec.queueInputBuffer(inputBufIndex, 0, 1024 * 1024, (1000000 * mEncodedSize / AUDIO_BYTE_PER_SAMPLE), 0);
            mEncodedSize += BUFFER_SIZE_IN_BYTES;
        }

        int outputBufIndex = mAudioEncoder.dequeueOutputBuffer(mOutBufferInfo, 1000);
        if (outputBufIndex >= 0) {
            mOutBuffer = mAudioOutputBuffers[outputBufIndex];
            if ((mOutBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                // 这里可以从ByteBuffer中获取csd参数
                mAudioEncoder.releaseOutputBuffer(outputBufIndex, false);
                return;
            }
            if (mOutBufferInfo.size != 0 && mLastAudioPresentationTimeUs < mOutBufferInfo.presentationTimeUs) {
                mOutBuffer.position(mOutBufferInfo.offset);
                mOutBuffer.limit(mOutBufferInfo.offset + mOutBufferInfo.size);

                addADTStoPacket(mADTSHeader, mOutBufferInfo.size + 7);
                mMixedFileOutputStream.write(mADTSHeader);

                mOutBuffer.get(mEncodedBytes, 0, mOutBufferInfo.size);
                mOutBuffer.clear();
                mMixedFileOutputStream.write(mEncodedBytes, 0, mOutBufferInfo.size);

                mLastAudioPresentationTimeUs = mOutBufferInfo.presentationTimeUs;
            }

            mAudioEncoder.releaseOutputBuffer(outputBufIndex, false);
        } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mAudioOutputBuffers = mAudioEncoder.getOutputBuffers();
        }
    }

    //record audio
    private void startRecording() {
        final AudioRecord recorder = createAudioRecord(mediaProjection);

        recorderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (LocalSocket socket = connect()) {
                    handler.sendEmptyMessage(MSG_CONNECTION_ESTABLISHED);

                    recorder.startRecording();
                    int BUFFER_MS = 15; // do not buffer more than BUFFER_MS milliseconds
                    byte[] buf = new byte[SAMPLE_RATE * CHANNELS * BUFFER_MS / 1000];
                    while (true) {
                        int r = recorder.read(buf, 0, buf.length);
                        socket.getOutputStream().write(buf, 0, r);
                    }
                } catch (IOException e) {
                    // ignore
                } finally {
                    recorder.stop();
                    stopSelf();
                }
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
}
