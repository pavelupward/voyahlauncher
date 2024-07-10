package com.simplemobiletools.applauncher.voyah

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.*
import android.widget.Button
import androidx.core.app.NotificationCompat
import com.simplemobiletools.applauncher.R
import com.simplemobiletools.applauncher.activities.SplashActivity


class VoyahFloatingButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingButton: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showNotification()
        addFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingButton == null) {
            addFloatingButton()
        }
        showNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(floatingButton)
        windowManager = null
        floatingButton = null
        layoutParams = null
    }


    private fun addFloatingButton() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val marginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 50.toFloat(), resources.displayMetrics
        ).toInt()
        floatingButton = inflater.inflate(R.layout.voyah_view_btn_float, null)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams?.let {
            it.gravity = Gravity.BOTTOM or Gravity.END
            it.x = marginInPx
            it.y = marginInPx
        }
        windowManager?.addView(floatingButton, layoutParams)

        val button: Button? = floatingButton?.findViewById(R.id.voyah_floating_button)
        button?.let {
            it.setOnTouchListener(FloatingButtonTouchListener())
            it.setOnClickListener {
                val intent = Intent(this, SplashActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

    }

    private fun showNotification() {
        val channelId = "${packageName}_voyah"
        val channelName = "custom_voyah_app_launcher"
        val notificationId = 1066661823

        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationChannel.setSound(null, null)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setTicker("Just for android")
            .setContentText("Just for android text")
            .setContentTitle("Just for android title")
            .setSmallIcon(R.drawable.ic_flag_chinese_cn_vector)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(notificationId, notification)
    }

    private inner class FloatingButtonTouchListener : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val btnParams = layoutParams ?: return false
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    initialX = btnParams.x
                    initialY = btnParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    btnParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    btnParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingButton, btnParams)
                    return true
                }

            }
            return false
        }
    }

}
