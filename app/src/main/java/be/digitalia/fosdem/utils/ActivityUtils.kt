package be.digitalia.fosdem.utils

import android.app.Activity
import android.os.Build
import androidx.annotation.AnimRes

object ActivityUtils {
    const val OVERRIDE_TRANSITION_OPEN = 0
    const val OVERRIDE_TRANSITION_CLOSE = 1

    @Suppress("DEPRECATION")
    fun overrideActivityTransition(
        activity: Activity,
        overrideType: Int,
        @AnimRes enterAnim: Int,
        @AnimRes exitAnim: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(overrideType, enterAnim, exitAnim)
        } else {
            activity.overridePendingTransition(enterAnim, exitAnim)
        }
    }
}