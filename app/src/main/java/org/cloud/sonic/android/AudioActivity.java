package org.cloud.sonic.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

/**
 * @author Eason, sndcpy
 * More https://github.com/rom1v/sndcpy
 */
public class AudioActivity extends Activity {

    private static final int REQUEST_CODE_PERMISSION_AUDIO = 1;
    private static final int REQUEST_CODE_START_CAPTURE = 2;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.RECORD_AUDIO};
            requestPermissions(permissions, REQUEST_CODE_PERMISSION_AUDIO);
        }

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_START_CAPTURE);
    }

    @SuppressLint("NewApi")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_START_CAPTURE && resultCode == Activity.RESULT_OK) {
            AudioService.start(this, data);
        }
        //exit
        finish();
    }
}
