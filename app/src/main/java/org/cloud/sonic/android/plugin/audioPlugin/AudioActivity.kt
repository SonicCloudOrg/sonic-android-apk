package org.cloud.sonic.android.plugin.audioPlugin

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import org.cloud.sonic.android.plugin.SonicPluginAudioService

class AudioActivity : Activity() {
    private val REQUEST_CODE_START_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_CODE_START_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_START_CAPTURE && resultCode == RESULT_OK) {
            SonicPluginAudioService.start(this, data)
        }
        //exit
        finish()
    }
}