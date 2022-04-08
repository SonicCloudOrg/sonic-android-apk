package org.cloud.sonic.android.plugin

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.*
import android.media.MediaCodec.CodecException
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.os.*
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.LogUtils
import org.cloud.sonic.android.R
import org.cloud.sonic.android.utils.ADTSUtil
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class SonicPluginAudioService:Service() {

    companion object{
        private val EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData"
        private val ACTION_RECORD = "org.cloud.sonic.android.RECORD"
        private val ACTION_STOP = "org.cloud.sonic.android.STOP"
        private val MSG_CONNECTION_ESTABLISHED = 1
        private val NOTIFICATION_ID = 2

        @SuppressLint("NewApi")
        fun start(context: Context, data: Intent?) {
            val intent = Intent(context, SonicPluginAudioService::class.java)
            intent.action = ACTION_RECORD
            intent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            }else{
                context.startService(intent)
            }
        }
    }

    private val CHANNEL_ID = "sonicaudioservice"

    private val ACTION_RECORD = "org.cloud.sonic.android.RECORD"
    private val ACTION_STOP = "org.cloud.sonic.android.STOP"


    private val SAMPLE_RATE = 44100
    private val CHANNELS = 2

    private val handler: Handler = ConnectionHandler(this)
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null

    // 处理 AAC 录制音频
    private var mMediaCodec: MediaCodec? = null
    private var mAudioRecord: AudioRecord? = null

    private var workThread: Thread? = null

    //开启 LocalServiceSocket 的服务器
    private var serverSocket: LocalServerSocket? = null
    private var clientSocket: LocalSocket? = null
    private var outputStream: OutputStream? = null
    private var mInputStream: InputStream? = null

    //超时时间 30s
    val LINK_SOCKET_TIMEOUT = 30 * 1000

    //定义在ActivityManagerService中，Service超时消息的what值
    val LINK_SOCKET_TIMEOUT_MSG = 0
    val REC_SERVICE_ACTION = 1

    private val mBufferSize = 4 * 1024

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                LINK_SOCKET_TIMEOUT_MSG -> {
                    stopSelf()
                }
                REC_SERVICE_ACTION -> {
                    val recMes = msg.obj as String
                    if (ACTION_STOP == recMes) {
                        stopSelf()
                    }
                }
                else -> {
                    Log.e("AudioService", "why are you here?")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()
        val notification = createNotification(false)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_NONE
        )
        getNotificationManager().createNotificationChannel(channel)
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationWithAndroidO(established: Boolean): Notification {
        val notificationBuilder =
            Notification.Builder(this, CHANNEL_ID)
        notificationBuilder.setContentTitle(getString(R.string.welcome))
        val textRes: Int =
            if (established) R.string.manager_notification_forwarding else R.string.notification_waiting
        notificationBuilder.setContentText(getText(textRes))
        notificationBuilder.setSmallIcon(R.mipmap.logo)
        notificationBuilder.addAction(createStopActionWithAndroidO())
        return notificationBuilder.build()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun createStopActionWithAndroidO(): Notification.Action {
        val stopIntent: Intent = createStopIntent()
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIcon = Icon.createWithResource(this, R.mipmap.logo)
        val stopString = getString(R.string.action_stop)
        val actionBuilder = Notification.Action.Builder(stopIcon, stopString, stopPendingIntent)
        return actionBuilder.build()
    }

    private fun createStopIntent(): Intent {
        val intent = Intent(this, SonicPluginAudioService::class.java)
        intent.action = ACTION_STOP
        return intent
    }

    private fun getNotificationManager(): NotificationManager {
        return getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun createNotification(established: Boolean): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        notificationBuilder.setContentTitle(getString(R.string.welcome_audio))
        val textRes: Int =
            if (established) R.string.manager_notification_forwarding else R.string.notification_waiting
        notificationBuilder.setSmallIcon(R.mipmap.logo)
        notificationBuilder.setContentText(getText(textRes))
        notificationBuilder.addAction(createStopAction())
        return notificationBuilder.build()
    }

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent: Intent = createStopIntent()
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIcon = R.mipmap.logo
        val stopString = getString(R.string.action_stop)
        val actionBuilder =
            NotificationCompat.Action.Builder(stopIcon, stopString, stopPendingIntent)
        return actionBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent!!.action
        if (ACTION_STOP == action) {
            //exit
            stopSelf()
            return START_NOT_STICKY
        }

        val data =
            intent.getParcelableExtra<Intent>(EXTRA_MEDIA_PROJECTION_DATA)
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager?.getMediaProjection(Activity.RESULT_OK, data!!)

        if (mediaProjection != null) {
            startRecording()
            //必须要在子线程里接收消息
            Thread { this.acceptMsg() }.start()
        } else {
            LogUtils.w("Failed to capture audio")
            stopSelf()
        }
        linkTimeOutStop()
        return START_NOT_STICKY
    }

    private fun linkTimeOutStop() {
        val msg = mHandler.obtainMessage(
            LINK_SOCKET_TIMEOUT_MSG
        )
        msg.obj = "LINK_SOCKET_TIMEOUT"
        //当超时后仍没有remove该SERVICE_TIMEOUT_MSG消息，则执行service Timeout流程【见2.3.1】
        mHandler.sendMessageDelayed(
            msg,
            LINK_SOCKET_TIMEOUT.toLong()
        )
        //在这里，把加入的延时消息给移除掉了
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createAudioPlaybackCaptureConfig(mediaProjection: MediaProjection): AudioPlaybackCaptureConfiguration {
        val confBuilder = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
        confBuilder.addMatchingUsage(AudioAttributes.USAGE_MEDIA)
        confBuilder.addMatchingUsage(AudioAttributes.USAGE_GAME)
        confBuilder.addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
        return confBuilder.build()
    }

    private fun createAudioFormat(): AudioFormat {
        val builder = AudioFormat.Builder()
        //raw pcm 16bit
        builder.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        builder.setSampleRate(SAMPLE_RATE)
        builder.setChannelMask(if (CHANNELS == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO)
        return builder.build()
    }

    @SuppressLint("NewApi", "MissingPermission")
    private fun createAudioRecord(mediaProjection: MediaProjection): AudioRecord? {
        val builder = AudioRecord.Builder()
        builder.setAudioFormat(createAudioFormat())
        builder.setBufferSizeInBytes(1024 * 1024)
        builder.setAudioPlaybackCaptureConfig(
            createAudioPlaybackCaptureConfig(
                mediaProjection
            )
        )
        return builder.build()
    }

    //record audio
    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun startRecording() {
        workThread = object : Thread("publish-thread") {
            override fun run() {
                try {
                    LogUtils.i(
                        String.format(
                            "creating socket %s",
                            CHANNEL_ID
                        )
                    )
                    serverSocket =
                        LocalServerSocket(CHANNEL_ID)
                    LogUtils.i(
                        String.format(
                            "Listening on %s",
                            CHANNEL_ID
                        )
                    )
                    clientSocket = serverSocket!!.accept()
                    LogUtils.d("client connected")
                    outputStream = clientSocket!!.outputStream
                    //将之前埋的 30 秒炸弹关闭
                    mHandler.removeMessages(LINK_SOCKET_TIMEOUT_MSG)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        workThread?.start()
        mediaProjection?.let {
            mAudioRecord = createAudioRecord(it)
            mAudioRecord?.startRecording()
            recordInternalAudio(mAudioRecord!!)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun recordInternalAudio(audioRecord: AudioRecord) {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val mediaFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                SAMPLE_RATE,
                CHANNELS
            )
            mediaFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 196000)
            mediaFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            ADTSUtil.initADTS(SAMPLE_RATE.toLong(), CHANNELS.toLong())
            mMediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val totalBytesRead = intArrayOf(0)
            val mPresentationTime = arrayOf(0L)
            mMediaCodec?.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(@NonNull mediaCodec: MediaCodec, i: Int) {
                    val codecInputBuffer = mediaCodec.getInputBuffer(i)
                    val capacity = codecInputBuffer!!.capacity()
                    val buffer = ByteArray(capacity)
                    val readBytes = audioRecord.read(buffer, 0, buffer.size)
                    if (readBytes > 0) {
                        codecInputBuffer.put(buffer, 0, readBytes)
                        mediaCodec.queueInputBuffer(i, 0, readBytes, mPresentationTime[0], 0)
                        totalBytesRead[0] += readBytes
                        mPresentationTime[0] = 1000000L * (totalBytesRead[0] / 2) / 44100
                    }
                }

                override fun onOutputBufferAvailable(
                    @NonNull codec: MediaCodec,
                    outputBufferIndex: Int,
                    @NonNull mBufferInfo: MediaCodec.BufferInfo
                ) {
                    if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                        LogUtils.i("AudioService", "AAC的配置数据")
                    } else {
                        val oneADTSFrameBytes = ByteArray(7 + mBufferInfo.size)
                        ADTSUtil.addADTS(oneADTSFrameBytes)
                        val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                        outputBuffer!![oneADTSFrameBytes, 7, mBufferInfo.size]
                        if (outputStream != null) {
                            try {
                                outputStream!!.write(oneADTSFrameBytes, 0, oneADTSFrameBytes.size)
                                outputStream!!.flush()
                            } catch (e: IOException) {
                                stopSelf()
                                e.printStackTrace()
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                }

                override fun onError(@NonNull mediaCodec: MediaCodec, @NonNull e: CodecException) {
                    e.printStackTrace()
                    stopSelf()
                }

                override fun onOutputFormatChanged(
                    @NonNull mediaCodec: MediaCodec,
                    @NonNull mediaFormat: MediaFormat
                ) {
                }
            })
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mMediaCodec!!.start()
    }

    fun isRunning(): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        mAudioRecord?.release()
        mMediaCodec?.stop()
        mMediaCodec?.release()
        disSocketService()
        stopForeground(true)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun acceptMsg() {
        while (true) {
            if (clientSocket != null) {
                try {
                    val buffer = ByteArray(1024)
                    mInputStream = clientSocket!!.inputStream
                    val count = mInputStream?.read(buffer)?:0
                    val key = String(Arrays.copyOfRange(buffer, 0, count))
                    LogUtils.d(
                        "ServerActivity mSocketOutStream==$key"
                    )
                    val msg =
                        mHandler.obtainMessage(REC_SERVICE_ACTION)
                    msg.obj = key
                    msg.sendToTarget()
                } catch (e: IOException) {
                    LogUtils.d(
                        "exception==" + e.fillInStackTrace().message
                    )
                    e.printStackTrace()
                }
            }
        }
    }

    private fun disSocketService() {
        try {
            serverSocket?.let {
                it.close()
                serverSocket = null
            }
            clientSocket?.let {
                it.outputStream.close()
                it.close()
                clientSocket = null
            }
            outputStream = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class ConnectionHandler internal constructor(var service: SonicPluginAudioService) :
        Handler() {

        @RequiresApi(api = Build.VERSION_CODES.O)
        override fun handleMessage(message: Message) {
            if (!service.isRunning()) {
                return
            }
            if (message.what == MSG_CONNECTION_ESTABLISHED) {
                val notification: Notification = service.createNotification(true)
                service.getNotificationManager()
                    .notify(NOTIFICATION_ID, notification)
            }
        }

        init {
            this.service = service
        }
    }

    override fun onBind(intent: Intent?): IBinder? { return null }
}