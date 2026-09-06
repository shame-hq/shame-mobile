package com.shame.tracker.ui.settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.shame.tracker.R
import com.shame.tracker.ShameTrackerApp

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("dark_mode")?.setOnPreferenceChangeListener { _, newValue ->
            ShameTrackerApp.applyDarkMode(newValue as String)
            true
        }
    }
}
