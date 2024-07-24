package com.simplemobiletools.applauncher.voyah

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

class VoyahInitBootReceiver : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == null || p1.action == Intent.ACTION_BOOT_COMPLETED) {
            p0?.let {
                if (Settings.canDrawOverlays(p0)) {
                    val serviceIntent = Intent(p0, VoyahFloatingButtonService::class.java)
                    p0.startForegroundService(serviceIntent)
                    Log.d("OverlayPermission", "SYSTEM_ALERT_WINDOW permission granted")
                }
            }
        }
    }
}
