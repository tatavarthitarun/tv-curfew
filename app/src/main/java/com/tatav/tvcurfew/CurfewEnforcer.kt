package com.tatav.tvcurfew

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

/** Central place that decides whether to lock, and performs the lock. */
object CurfewEnforcer {
    const val TAG = "TvCurfew"

    fun admin(context: Context) = ComponentName(context, CurfewAdminReceiver::class.java)

    private fun dpm(context: Context) =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isAdminActive(context: Context): Boolean =
        dpm(context).isAdminActive(admin(context))

    /** Lock unconditionally (used by the "Test lock now" button). */
    fun forceLock(context: Context): Boolean {
        if (!isAdminActive(context)) {
            Log.w(TAG, "forceLock: device admin not active")
            return false
        }
        Log.i(TAG, "forceLock: lockNow()")
        dpm(context).lockNow()
        return true
    }

    /** Lock only if we are currently inside the curfew window. */
    fun enforceIfNeeded(context: Context, reason: String): Boolean {
        if (!CurfewLogic.isCurfewNow()) {
            Log.d(TAG, "enforce($reason): outside curfew - allowed")
            return false
        }
        if (!isAdminActive(context)) {
            Log.w(TAG, "enforce($reason): IN curfew but admin not active - cannot lock")
            return false
        }
        Log.i(TAG, "enforce($reason): IN curfew -> lockNow()")
        dpm(context).lockNow()
        return true
    }
}
