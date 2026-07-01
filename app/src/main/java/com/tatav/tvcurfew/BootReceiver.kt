package com.tatav.tvcurfew

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Restarts the curfew service after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(CurfewEnforcer.TAG, "boot/replace: ${intent.action} -> start service")
        MainActivity.startCurfew(context)
    }
}
