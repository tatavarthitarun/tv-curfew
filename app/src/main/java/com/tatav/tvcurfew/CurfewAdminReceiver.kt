package com.tatav.tvcurfew

import android.app.admin.DeviceAdminReceiver

/**
 * Device-admin component. Enabling it grants the app the FORCE_LOCK policy,
 * which is what lets DevicePolicyManager.lockNow() put the TV into standby.
 */
class CurfewAdminReceiver : DeviceAdminReceiver()
