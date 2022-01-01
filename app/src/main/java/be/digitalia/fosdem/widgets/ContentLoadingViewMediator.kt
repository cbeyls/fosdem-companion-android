package be.digitalia.fosdem.widgets

import android.os.SystemClock
import android.view.View
import androidx.core.view.isVisible

/**
 * ContentLoadingViewMediator controls the visibility of a View. It waits a minimum time
 * to be dismissed before showing. Once visible, the view will be visible for
 * a minimum amount of time to avoid "flashes" in the UI when an event could take
 * a largely variable time to complete (from none, to a user perceivable amount).
 *
 * @author Christophe Beyls
 */
class ContentLoadingViewMediator(private val view: View) {

    init {
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                if (isVisible && !v.isVisible) {
                    v.postDelayed(delayedAction, MIN_DELAY)
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
                v.removeCallbacks(delayedAction)
                if (!isVisible && v.isVisible) {
                    v.isVisible = false
                }
                startTime = -1L
            }
        })
    }

    private val delayedAction = Runnable {
        val visible = isVisible
        view.isVisible = visible
        startTime = if (visible) SystemClock.uptimeMillis() else -1L
    }
    private var startTime = -1L

    var isVisible = view.isVisible
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (view.isAttachedToWindow) {
                view.removeCallbacks(delayedAction)

                if (value && startTime == -1L) {
                    // Show the view after waiting for a minimum delay. If during that time,
                    // isVisible switches to false, the view is never made visible.
                    view.postDelayed(delayedAction, MIN_DELAY)
                }
            }
            if (!value) {
                val diff = SystemClock.uptimeMillis() - startTime
                if (startTime == -1L || diff >= MIN_SHOW_TIME) {
                    // The view has been shown long enough OR
                    // was not shown yet. If it wasn't shown yet,
                    // it will just never be shown.
                    view.isVisible = false
                    startTime = -1L
                } else {
                    // The view is shown, but not long enough,
                    // so put a delayed message in to hide it
                    // when it's been shown long enough.
                    view.postDelayed(delayedAction, MIN_SHOW_TIME - diff)
                }
            }
        }

    companion object {
        private const val MIN_SHOW_TIME = 500L // ms
        private const val MIN_DELAY = 500L // ms
    }
}