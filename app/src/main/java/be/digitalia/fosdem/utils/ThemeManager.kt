package be.digitalia.fosdem.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

object ThemeManager {

    private val onSharedPreferenceChangeListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == PreferenceKeys.THEME) {
            updateTheme(sharedPreferences)
        }
    }

    fun init(context: Context) {
        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        updateTheme(defaultSharedPreferences)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    private fun updateTheme(sharedPreferences: SharedPreferences) {
        val mode = sharedPreferences.getString(PreferenceKeys.THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString())!!.toInt()
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}