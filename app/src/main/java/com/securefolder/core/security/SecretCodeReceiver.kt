package com.securefolder.core.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives secret dialer code broadcasts for stealth mode access.
 * Dial *#*#7378#*#* to launch the app when icon is hidden.
 */
class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent?.let { context.startActivity(it) }
    }
}
