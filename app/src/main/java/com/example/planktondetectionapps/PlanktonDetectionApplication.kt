package com.example.planktondetectionapps

import android.app.Application
import android.util.Log
import com.example.planktondetectionapps.auth.AuthManager

/**
 * Custom Application class for initializing app-wide components
 */
class PlanktonDetectionApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("PlanktonApp", "Application onCreate() called")

        // Initialize AuthManager with application context
        AuthManager.getInstance().initialize(this)

        Log.d("PlanktonApp", "AuthManager initialized successfully")
    }
}
