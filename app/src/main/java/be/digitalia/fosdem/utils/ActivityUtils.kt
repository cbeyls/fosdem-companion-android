package be.digitalia.fosdem.utils

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.AnimRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

object ActivityUtils {
    const val OVERRIDE_TRANSITION_OPEN = 0
    const val OVERRIDE_TRANSITION_CLOSE = 1

    /**
     * Call this method in Activity.onCreate() to configure the open or close transitions.
     */
    @Suppress("DEPRECATION")
    fun overrideActivityTransition(
        activity: ComponentActivity,
        overrideType: Int,
        @AnimRes enterAnim: Int,
        @AnimRes exitAnim: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(overrideType, enterAnim, exitAnim)
        } else {
            if (overrideType == OVERRIDE_TRANSITION_OPEN) {
                activity.overridePendingTransition(enterAnim, exitAnim)
            } else {
                activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE && activity.isFinishing) {
                        activity.overridePendingTransition(enterAnim, exitAnim)
                    }
                })
            }
        }
    }
}