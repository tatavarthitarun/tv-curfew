package com.tatav.tvcurfew

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Long-lived foreground service. Its whole job is to keep a runtime receiver for
 * ACTION_SCREEN_ON registered (that broadcast cannot be declared in the manifest since
 * Android 7). Whenever the TV wakes during curfew, it is locked straight back to standby.
 */
class CurfewService : Service() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    Log.d(CurfewEnforcer.TAG, "screen event: ${intent.action}")
                    CurfewEnforcer.enforceIfNeeded(this@CurfewService, "screen_on")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
        CurfewAlarmReceiver.scheduleNext(this)
        CurfewEnforcer.enforceIfNeeded(this, "service_start")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CurfewEnforcer.enforceIfNeeded(this, "start_command")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val channelId = "tv_curfew"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(channelId, "TV Curfew", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notif: Notification = builder
            .setContentTitle("TV Curfew active")
            .setContentText("Enforcing 9 PM to 10 AM standby")
            .setSmallIcon(R.drawable.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notif)
        }
    }
}
