package com.simplemobiletools.applauncher.voyah

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.*
import android.widget.Button
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(floatingButton)
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
