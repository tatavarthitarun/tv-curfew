package com.tatav.tvcurfew

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Kill-resistant watchdog. WorkManager persists its jobs across app death and reboots and
 * re-runs them, so even if an aggressive OEM power manager kills the foreground service and
 * cancels the alarms, this worker still fires (15-minute floor — WorkManager's minimum period).
 *
 * On each tick it:
 *   1. enforces the lock directly (works even with the service dead — lockNow() needs no service),
 *   2. best-effort restarts the instant-relock service (may be blocked from background on 12+; caught),
 *   3. re-arms the 60s/next-9PM alarm chain.
 */
class CurfewWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Log.d(CurfewEnforcer.TAG, "watchdog tick")
        CurfewEnforcer.enforceIfNeeded(applicationContext, "worker")
        try {
            MainActivity.startCurfew(applicationContext)
        } catch (e: Exception) {
            Log.w(CurfewEnforcer.TAG, "watchdog could not restart service: ${e.message}")
        }
        CurfewAlarmReceiver.scheduleNext(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "curfew_watchdog"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<CurfewWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.UPDATE, req
            )
            Log.d(CurfewEnforcer.TAG, "watchdog worker scheduled (15 min)")
        }
    }
}
