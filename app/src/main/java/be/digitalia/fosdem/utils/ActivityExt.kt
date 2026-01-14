package be.digitalia.fosdem.utils

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.AnimRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

enum class ActivityTransitionOverrideType {
    OPEN,
    CLOSE
}

/**
 * Call this method in Activity.onCreate() to configure the open or close transitions.
 */
@Suppress("DEPRECATION")
fun ComponentActivity.overrideActivityTransitionCompat(
    overrideType: ActivityTransitionOverrideType,
    @AnimRes enterAnim: Int,
    @AnimRes exitAnim: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(overrideType.ordinal, enterAnim, exitAnim)
    } else {
        when (overrideType) {
            ActivityTransitionOverrideType.OPEN -> overridePendingTransition(enterAnim, exitAnim)
            ActivityTransitionOverrideType.CLOSE -> lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE && isFinishing) {
                    overridePendingTransition(enterAnim, exitAnim)
                }
            })
        }
    }
}

private val DefaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DefaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

fun ComponentActivity.setupEdgeToEdge(isNavigationBarScrimEnabled: Boolean = true) {
    val navigationBarStyle = if (isNavigationBarScrimEnabled) {
        SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim)
    } else {
        SystemBarStyle.auto(
            lightScrim = Color.TRANSPARENT,
            // We still need to apply a scrim in light mode on API < 26 because the navigation bar is always dark
            darkScrim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ||
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            ) {
                Color.TRANSPARENT
            } else {
                DefaultDarkScrim
            }
        )
    }
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = navigationBarStyle
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isNavigationBarScrimEnabled) {
        window.isNavigationBarContrastEnforced = false
    }
}