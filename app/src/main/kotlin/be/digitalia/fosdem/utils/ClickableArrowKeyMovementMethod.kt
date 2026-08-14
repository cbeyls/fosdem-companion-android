package be.digitalia.fosdem.utils

import android.graphics.RectF
import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.text.getSpans

/**
 * An extension of ArrowKeyMovementMethod supporting clickable spans as well.
 */
object ClickableArrowKeyMovementMethod : ArrowKeyMovementMethod() {

    private val touchedLineBounds = RectF()

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        // If action has finished
        if (event.action == MotionEvent.ACTION_UP) {
            // Locate the area that was pressed
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            x += widget.scrollX
            y += widget.scrollY

            // Locate the text line
            val layout = widget.layout
            val line = layout.getLineForVertical(y)

            // Check that the touch actually happened within the line bounds
            with(touchedLineBounds) {
                left = layout.getLineLeft(line)
                top = layout.getLineTop(line).toFloat()
                right = layout.getLineWidth(line) + left
                bottom = layout.getLineBottom(line).toFloat()
            }

            if (touchedLineBounds.contains(x.toFloat(), y.toFloat())) {
                val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                // Find a clickable span at that text offset, if any
                val clickableSpans = buffer.getSpans<ClickableSpan>(offset, offset)
                if (clickableSpans.isNotEmpty()) {
                    clickableSpans[0].onClick(widget)
                    return true
                }
            }
        }

        return super.onTouchEvent(widget, buffer, event)
    }
}