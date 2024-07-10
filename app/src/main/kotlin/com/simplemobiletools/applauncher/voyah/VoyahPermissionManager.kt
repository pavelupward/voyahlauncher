package com.simplemobiletools.applauncher.voyah

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat

internal const val PERMISSION_REQUEST_CODE = 100
internal const val SYSTEM_ALERT_WINDOW_REQUEST_CODE = 101

fun checkAndRequestPermissions(context: AppCompatActivity) {
    val permissionsToRequest = mutableListOf<String>()

    val permissions = listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        "android.permission.WRITE_SECURE_SETTINGS",
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.WRITE_STORAGE",
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.FOREGROUND_SERVICE,
        "android.permission.FOREGROUND_SERVICE_MICROPHONE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.MEDIA_BUTTON",
        Manifest.permission.INTERNET,
        "android.permission.READ_LOGS",
        "android.permission.MANAGE_ACTIVITY_STACKS",
        Manifest.permission.GET_TASKS,
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(permission)
        }
    }

    if (permissionsToRequest.isNotEmpty()) {
        ActivityCompat.requestPermissions(context, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
    }
}

fun checkSpecialPermissions(activity: Activity) {
    if (!Settings.canDrawOverlays(activity)) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
        startActivityForResult(activity, intent, SYSTEM_ALERT_WINDOW_REQUEST_CODE, null)
    }
}
