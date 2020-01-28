package be.digitalia.fosdem.widgets

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible

/**
 * ContentLoadingProgressBar implements a ProgressBar that waits a minimum time to be
 * dismissed before showing. Once visible, the progress bar will be visible for
 * a minimum amount of time to avoid "flashes" in the UI when an event could take
 * a largely variable time to complete (from none, to a user perceivable amount).
 *
 *
 * This version is similar to the support library version but implemented "the right way".
 *
 * @author Christophe Beyls
 */
class ContentLoadingProgressBar : ProgressBar {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var shouldBeVisible: Boolean = isVisible
    private var startTime = -1L
    private val delayedHide = Runnable {
        isVisible = false
        startTime = -1L
    }
    private val delayedShow = Runnable {
        startTime = SystemClock.uptimeMillis()
        isVisible = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (shouldBeVisible && !isVisible) {
            postDelayed(delayedShow, MIN_DELAY)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(delayedHide)
        removeCallbacks(delayedShow)
        if (!shouldBeVisible && startTime != -1L) {
            isVisible = false
        }
        startTime = -1L
    }

    /**
     * Hide the progress view if it is visible. The progress view will not be
     * hidden until it has been shown for at least a minimum show time. If the
     * progress view was not yet visible, cancels showing the progress view.
     */
    fun hide() {
        if (shouldBeVisible) {
            shouldBeVisible = false
            if (ViewCompat.isAttachedToWindow(this)) {
                removeCallbacks(delayedShow)
            }
            val diff = SystemClock.uptimeMillis() - startTime
            if (startTime == -1L || diff >= MIN_SHOW_TIME) {
                // The progress spinner has been shown long enough
                // OR was not shown yet. If it wasn't shown yet,
                // it will just never be shown.
                isVisible = false
                startTime = -1L
            } else {
                // The progress spinner is shown, but not long enough,
                // so put a delayed message in to hide it when its been
                // shown long enough.
                postDelayed(delayedHide, MIN_SHOW_TIME - diff)
            }
        }
    }

    /**
     * Show the progress view after waiting for a minimum delay. If
     * during that time, hide() is called, the view is never made visible.
     */
    fun show() {
        if (!shouldBeVisible) {
            shouldBeVisible = true
            if (ViewCompat.isAttachedToWindow(this)) {
                removeCallbacks(delayedHide)
                if (startTime == -1L) {
                    postDelayed(delayedShow, MIN_DELAY)
                }
            }
        }
    }

    companion object {
        private const val MIN_SHOW_TIME = 500L // ms
        private const val MIN_DELAY = 500L // ms
    }
}