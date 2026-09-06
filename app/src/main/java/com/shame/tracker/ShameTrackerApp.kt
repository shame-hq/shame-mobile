package com.shame.tracker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShameTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        applyDarkMode(prefs.getString("dark_mode", "system") ?: "system")
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
