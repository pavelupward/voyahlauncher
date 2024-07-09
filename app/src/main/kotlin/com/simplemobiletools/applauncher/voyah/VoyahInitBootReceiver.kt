package com.simplemobiletools.applauncher.voyah

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class VoyahInitBootReceiver : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == Intent.ACTION_BOOT_COMPLETED) {
            p0?.let {
                val serviceIntent = Intent(it, VoyahFloatingButtonService::class.java)
                it.startService(serviceIntent)
            }
        }
    }
}
