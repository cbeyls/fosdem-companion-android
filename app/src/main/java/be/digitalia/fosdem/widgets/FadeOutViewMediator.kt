package be.digitalia.fosdem.widgets

import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.addListener
import androidx.core.view.isVisible

/**
 * Hide a view with a fade out animation.
 * Cancel the animation if the view needs to be visible again.
 */
class FadeOutViewMediator(private val view: View) {

    private val fadeOutAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
        duration = 500L
        addListener(
                onStart = {
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                },
                onEnd = {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    view.alpha = 1f
                    if (!isVisible) {
                        view.isVisible = false
                    }
                }
        )
    }

    var isVisible = view.isVisible
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value) {
                if (fadeOutAnimator.isRunning) {
                    fadeOutAnimator.cancel()
                } else {
                    view.isVisible = true
                }
            } else {
                fadeOutAnimator.start()
            }
        }
}