package com.securefolder.core.security

import android.app.Activity
import android.view.WindowManager

/**
 * Screen protection utilities.
 * Prevents screenshots, screen recording, and task switcher previews.
 */
object ScreenProtection {

    /**
     * Enable FLAG_SECURE to prevent screenshots and screen recording.
     * Also prevents content from appearing in task switcher.
     */
    fun enableScreenProtection(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * Disable screen protection (if needed for specific screens).
     */
    fun disableScreenProtection(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
