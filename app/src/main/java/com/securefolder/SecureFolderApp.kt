package com.securefolder

import android.app.Application
import android.view.WindowManager
import com.securefolder.core.security.SecurityManager

/**
 * Secure Folder Application class.
 * Initializes security measures at app startup.
 */
class SecureFolderApp : Application() {

    lateinit var securityManager: SecurityManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        securityManager = SecurityManager(this)
        securityManager.performStartupSecurityChecks()
    }

    companion object {
        lateinit var instance: SecureFolderApp
            private set
    }
}
