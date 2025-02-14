package be.digitalia.fosdem.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.google.android.material.R as MaterialR

@SuppressLint("PrivateResource")
fun CustomTabsIntent.Builder.configureColorSchemes(
    context: Context,
    @ColorRes toolbarColorResId: Int
): CustomTabsIntent.Builder {
    val defaultColorSchemeParams = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(ContextCompat.getColor(context, toolbarColorResId))
        .setNavigationBarColor(ContextCompat.getColor(context, MaterialR.color.design_default_color_background))
        .build()
    val darkColorSchemeParams = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(ContextCompat.getColor(context, MaterialR.color.design_dark_default_color_surface))
        .setNavigationBarColor(ContextCompat.getColor(context, MaterialR.color.design_dark_default_color_background))
        .build()

    // Request the browser tab to follow the app theme setting
    val colorScheme = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_NO -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        AppCompatDelegate.MODE_NIGHT_YES -> CustomTabsIntent.COLOR_SCHEME_DARK
        else -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
    }

    return setDefaultColorSchemeParams(defaultColorSchemeParams)
        .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkColorSchemeParams)
        .setColorScheme(colorScheme)
}