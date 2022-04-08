package org.cloud.sonic.android.plugin.activityPlugin

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.blankj.utilcode.util.LogUtils
import java.lang.reflect.InvocationTargetException

class SearchActivity : Activity() {

    companion object{
        const val EXTRA_SERIAL = "serial"
    }

    private interface SecuredGetter<T> {
        fun get(): T
    }

    private fun getSecuredId(supplier: SecuredGetter<String>): String {
        return try {
            supplier.get()
        } catch (e: SecurityException) {
            "secured"
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent

        val layout = LinearLayout(this)
        layout.keepScreenOn = true
        layout.orientation = LinearLayout.VERTICAL
        layout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        layout.setBackgroundColor(Color.parseColor("#409EFF"))
        layout.setPadding(16, 16, 16, 16)
        layout.gravity = Gravity.CENTER

        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        var serial = intent.getStringExtra(SearchActivity.EXTRA_SERIAL)

        if (serial == null) {
            serial = getProperty("ro.serialno", "unknown")
        }

        layout.addView(createTitleLabel())
        layout.addView(createLabel("SERIAL"))
        layout.addView(createData(serial!!))
        layout.addView(createLabel("MODEL"))
        layout.addView(createData(getProperty("ro.product.model", "unknown")))
        layout.addView(createLabel("VERSION"))
        layout.addView(createData(Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"))
        layout.addView(createLabel("OPERATOR"))
        layout.addView(createData(tm.simOperatorName))
        layout.addView(createLabel("PHONE"))
        layout.addView(createData(getSecuredId(object : SecuredGetter<String> {
            override fun get(): String {
                return tm.line1Number
            }
        })))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layout.addView(createLabel("IMEI"))
            layout.addView(createData(getSecuredId(object : SecuredGetter<String> {
                override fun get(): String {
                    return tm.imei
                }
            })))
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        ensureVisibility()
        setContentView(layout)
    }


    private fun createLabel(text: String): View? {
        val titleView = TextView(this)
        titleView.gravity = Gravity.CENTER
        titleView.setTextColor(Color.parseColor("#000000"))
        titleView.textSize = 16f
        titleView.text = text
        return titleView
    }

    private fun createTitleLabel(): View? {
        val titleView = TextView(this)
        titleView.gravity = Gravity.CENTER
        titleView.setPadding(16, 16, 16, 40)
        titleView.setTextColor(Color.WHITE)
        titleView.textSize = 35f
        titleView.text = "我在这里\nI am here"
        return titleView
    }

    private fun createData(text: String): View? {
        val dataView = TextView(this)
        dataView.gravity = Gravity.CENTER
        dataView.setTextColor(Color.WHITE)
        dataView.textSize = 24f
        dataView.text = text
        return dataView
    }

    private fun ensureVisibility() {
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        unlock()
        val params = window.attributes
        params.screenBrightness = 1.0f
        window.attributes = params
    }

    private fun getProperty(name: String, defaultValue: String): String {
        return try {
            val SystemProperties = Class.forName("android.os.SystemProperties")
            val get = SystemProperties.getMethod(
                "get",
                String::class.java,
                String::class.java
            )
            get.invoke(SystemProperties, name, defaultValue) as String
        } catch (e: ClassNotFoundException) {
            LogUtils.e("Class.forName() failed", e)
            defaultValue
        } catch (e: NoSuchMethodException) {
            LogUtils.e("getMethod() failed", e)
            defaultValue
        } catch (e: InvocationTargetException) {
            LogUtils.e("invoke() failed", e)
            defaultValue
        } catch (e: IllegalAccessException) {
            LogUtils.e("invoke() failed", e)
            defaultValue
        }
    }

    private fun unlock() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.newKeyguardLock("InputService/Unlock").disableKeyguard()
    }

    class IntentBuilder {
        @Nullable
        private var serial: String? = null
        fun serial(@NonNull serial: String?): IntentBuilder {
            this.serial = serial
            return this
        }

        fun build(context: Context): Intent {
            val intent = Intent(context.applicationContext, SearchActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (serial != null) {
                intent.putExtra(SearchActivity.EXTRA_SERIAL, serial)
            }
            return intent
        }
    }
}