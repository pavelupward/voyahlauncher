package com.simplemobiletools.applauncher.voyah

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.applauncher.activities.MainActivity

class VoyahInitBootReceiver : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == Intent.ACTION_BOOT_COMPLETED) {
            p0?.let {
                val intent = Intent(p0, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                p0.startActivity(intent)
            }
        }
    }
}
