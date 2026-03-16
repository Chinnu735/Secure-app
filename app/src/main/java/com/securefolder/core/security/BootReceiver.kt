package com.securefolder.core.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Ensures app is locked after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // App will require authentication on next launch
            // No action needed - app starts locked by default
        }
    }
}
