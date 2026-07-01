package com.tatav.tvcurfew

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

/**
 * Backstop enforcement. While OUTSIDE curfew it schedules a single exact alarm for the
 * next 9 PM. While INSIDE curfew it re-arms itself every 60s, so the TV is forced back
 * to standby throughout the whole window even if a screen-on broadcast is ever missed.
 */
class CurfewAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(CurfewEnforcer.TAG, "alarm fired")
        CurfewEnforcer.enforceIfNeeded(context, "alarm")
        scheduleNext(context)
    }

    companion object {
        private const val REQ = 42
        private const val HEARTBEAT_MS = 60_000L

        fun scheduleNext(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context, REQ,
                Intent(context, CurfewAlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt: Long = if (CurfewLogic.isCurfewNow()) {
                System.currentTimeMillis() + HEARTBEAT_MS
            } else {
                val now = Calendar.getInstance()
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, CurfewLogic.START_HOUR)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
                }.timeInMillis
            }

            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
            Log.d(CurfewEnforcer.TAG, "next alarm at ${java.util.Date(triggerAt)}")
        }
    }
}
