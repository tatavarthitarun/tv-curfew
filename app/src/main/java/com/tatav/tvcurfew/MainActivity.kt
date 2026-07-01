package com.tatav.tvcurfew

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Minimal setup screen. On a fresh install you: (1) tap "Enable Device Admin",
 * (2) the service auto-starts. After that the app runs headless forever.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(80, 80, 80, 80)
        }
        root.addView(TextView(this).apply { text = "TV Curfew"; textSize = 34f })
        status = TextView(this).apply { textSize = 18f; setPadding(0, 24, 0, 40) }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "1.  Enable Device Admin (required once)"
            setOnClickListener { requestAdmin() }
        })
        root.addView(Button(this).apply {
            text = "2.  Start / restart curfew service"
            setOnClickListener { startCurfew(this@MainActivity); refresh() }
        })
        root.addView(Button(this).apply {
            text = "Test:  lock the TV right now"
            setOnClickListener { CurfewEnforcer.forceLock(this@MainActivity) }
        })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (CurfewEnforcer.isAdminActive(this)) startCurfew(this)
    }

    private fun refresh() {
        val admin = CurfewEnforcer.isAdminActive(this)
        status.text = buildString {
            append("Device admin: ").append(if (admin) "ACTIVE" else "NOT active - tap step 1").append('\n')
            append("Curfew window: 9:00 PM  ->  10:00 AM\n")
            append("In curfew right now: ").append(if (CurfewLogic.isCurfewNow()) "YES" else "no")
        }
    }

    private fun requestAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, CurfewEnforcer.admin(this@MainActivity))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "TV Curfew needs lock permission to put the TV into standby during curfew hours."
            )
        }
        startActivity(intent)
    }

    companion object {
        fun startCurfew(context: Context) {
            // Arm the kill-resistant watchdog first, so it survives even if the FGS
            // start below is blocked from the background (Android 12+).
            CurfewWorker.schedule(context)
            val i = Intent(context, CurfewService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }
    }
}
