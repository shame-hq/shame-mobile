package com.shame.tracker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.shame.tracker.util.SyncScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShameTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        applyDarkMode(prefs.getString("dark_mode", "system") ?: "system")
        // Schedule background sync every 6 hours
        SyncScheduler.schedulePeriodic(this)
    }

    companion object {
        fun applyDarkMode(value: String) {
            val mode = when (value) {
                "light"  -> AppCompatDelegate.MODE_NIGHT_NO
                "dark"   -> AppCompatDelegate.MODE_NIGHT_YES
                else     -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
