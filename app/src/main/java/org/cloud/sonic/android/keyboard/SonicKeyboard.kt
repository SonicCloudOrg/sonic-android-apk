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

package org.cloud.sonic.android.keyboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.inputmethodservice.InputMethodService
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import org.cloud.sonic.android.R

/**
 *  @author : jeffery
 *  @date : 2023/4/17 19:56
 *  @email : jayw2016@outlook.com
 *  @github : https://github.com/wzasd
 *  description : Sonic Keyboard and clipboard
 */
class SonicKeyboard : InputMethodService() {
    companion object {
        private const val TAG = "SonicKeyboard"

        const val IME_RECOVER_MESSAGE = "SONIC_KEYBOARD"

        private const val IME_RECOVER_CLIPBOARD_GET = "SONIC_CLIPPER_GET"
        private const val IME_RECOVER_CLIPBOARD_SET = "SONIC_CLIPPER_SET"

        private const val EXTRA_MSG = "msg"
    }

    var mReceiver: BroadcastReceiver? = null

    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        val mInputView: View = layoutInflater.inflate(R.layout.view, null)
        if (mReceiver == null) {
            val filter = IntentFilter(IME_RECOVER_MESSAGE)
            filter.addAction(IME_RECOVER_CLIPBOARD_GET)
            filter.addAction(IME_RECOVER_CLIPBOARD_SET)
            mReceiver = AdbReceiver()
            registerReceiver(mReceiver, filter)
        }
        mInputView.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        return mInputView
    }

    override fun onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
        }
        super.onDestroy()

    }

    inner class AdbReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            LogUtils.d(TAG, intent.toString())
            when (intent.action) {
                IME_RECOVER_MESSAGE -> {
                    val msg = intent.getStringExtra(EXTRA_MSG)
                    val result = msg?.split("CODE_AC_ENTER|CODE_AC_BACK|CODE_AC_CLEAN")
                    result?.let {
                        for (r in it) {
                            if (r == "CODE_AC_ENTER") {
                                if (!sendDefaultEditorAction(true)) {
                                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                                }
                            } else if (r == "CODE_AC_BACK") {
                                currentInputConnection.also { ic: InputConnection ->
                                    ic.deleteSurroundingText(1, 0)
                                }
                            } else if (r == "CODE_AC_CLEAN") {
                                currentInputConnection.also { ic: InputConnection ->
                                    val curPos: CharSequence =
                                        ic.getExtractedText(ExtractedTextRequest(), 0).text
                                    val beforePos: CharSequence? =
                                        ic.getTextBeforeCursor(curPos.length, 0)
                                    val afterPos: CharSequence? =
                                        ic.getTextAfterCursor(curPos.length, 0)
                                    if (beforePos != null && afterPos != null) {
                                        ic.deleteSurroundingText(beforePos.length, afterPos.length)
                                    }
                                }
                            } else {
                                currentInputConnection.also { ic: InputConnection ->
                                    ic.commitText(r, 1)
                                }
                            }
                        }
                    }
                }
                IME_RECOVER_CLIPBOARD_GET -> {
                    processClipboardGet()
                }
                IME_RECOVER_CLIPBOARD_SET -> {
                    processClipboardSet(intent)
                }
            }
        }

        /**
         * Return the text for clipboard.
         */
        private fun processClipboardGet ()    {
            val cm = Utils.getApp().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(Utils.getApp())
                if (text != null) {
                    resultCode = Activity.RESULT_OK
                    resultData = text.toString()
                    return
                }
            }
            resultCode = Activity.RESULT_CANCELED
            resultData = ""
        }

        /**
         * Copy the text to clipboard.
         */
        private fun processClipboardSet(intent: Intent){
            val text = intent.getStringExtra(EXTRA_MSG)
            if(text != null) {
                val cm = Utils.getApp().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(Utils.getApp().packageName, text))
                resultCode = Activity.RESULT_OK
                resultData = "Text is copied into clipboard."
                return
            }
            resultCode = Activity.RESULT_CANCELED
            resultData = "No text is provided. Use -e text \"text to be pasted\""
        }
    }
}