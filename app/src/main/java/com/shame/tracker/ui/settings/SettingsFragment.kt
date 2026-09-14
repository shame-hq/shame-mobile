package com.shame.tracker.ui.settings

import android.os.Bundle
import android.view.View
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Background comes from the theme (bg_light_canvas or bg_dark_canvas)
        listView.apply {
            setPadding(24, 24, 24, 36)
            clipToPadding = false
        }
    }
}
