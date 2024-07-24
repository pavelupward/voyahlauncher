package com.simplemobiletools.applauncher.voyah

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.NotificationCompat
import com.simplemobiletools.applauncher.R
import com.simplemobiletools.applauncher.activities.MainActivity
import kotlin.math.abs


class VoyahFloatingButtonService : Service() {

    companion object {
        var isServiceRunning = false
    }

    private var windowManager: WindowManager? = null
    private var floatingButton: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (floatingButton == null) {
            addFloatingButton()
            showNotification()
        }
        Log.d("VoyahService", "Service onCreate called")
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingButton == null) {
            addFloatingButton()
            showNotification()
        }
        Log.d("VoyahService", "Service onStartCommand called")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(floatingButton)
        windowManager = null
        floatingButton = null
        layoutParams = null
        isServiceRunning = false
        Log.d("VoyahService", "Service destroyed")
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams?.let {
            it.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            it.x = marginInPx
            it.y = marginInPx
        }
        windowManager?.addView(floatingButton, layoutParams)

        val button: AppCompatImageButton? = floatingButton?.findViewById(R.id.voyah_floating_button)
        button?.let {
            it.setOnTouchListener(FloatingButtonTouchListener())
            it.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
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
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.setSound(null, null)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setTicker("Just for android")
            .setCategory(null)
            .setLocalOnly(false)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentText("Just for android text")
            .setContentTitle("Just for android title")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(notificationId, notification)
    }

    private inner class FloatingButtonTouchListener : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var isDragging = false
        private val CLICK_DRAG_TOLERANCE = 10f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val btnParams = layoutParams ?: return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = btnParams.x
                    initialY = btnParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    val newX = initialX + deltaX
                    val newY = initialY + deltaY

                    val displayMetrics = resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val screenHeight = displayMetrics.heightPixels

                    btnParams.x = Math.max(0, Math.min(newX, screenWidth - view.width))
                    btnParams.y = Math.max(0, Math.min(newY, screenHeight - view.height))

                    windowManager?.updateViewLayout(floatingButton, btnParams)
                    isDragging = true
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    val upRawX = event.rawX
                    val upRawY = event.rawY
                    val upDX = upRawX - initialTouchX
                    val upDY = upRawY - initialTouchY

                    if (abs(upDX) < CLICK_DRAG_TOLERANCE && abs(upDY) < CLICK_DRAG_TOLERANCE) {
                        view.post {
                            val intent = Intent(this@VoyahFloatingButtonService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        }
                    }
                    return true
                }

            }
            return false
        }
    }
}
