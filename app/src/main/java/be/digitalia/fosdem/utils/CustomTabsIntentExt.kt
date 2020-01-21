package be.digitalia.fosdem.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import be.digitalia.fosdem.R

@SuppressLint("PrivateResource")
fun CustomTabsIntent.Builder.configureToolbarColors(context: Context,
                                                    @ColorRes toolbarColorResId: Int): CustomTabsIntent.Builder {
    val darkColorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.design_dark_default_color_surface))
            .build()

    // Request the browser tab to follow the app theme setting
    val colorScheme = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_NO -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        AppCompatDelegate.MODE_NIGHT_YES -> CustomTabsIntent.COLOR_SCHEME_DARK
        else -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
    }

    return setToolbarColor(ContextCompat.getColor(context, toolbarColorResId))
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkColorSchemeParams)
            .setColorScheme(colorScheme)
}