package be.digitalia.fosdem.utils;

import android.graphics.RectF;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * An extension of ArrowKeyMovementMethod supporting clickable spans as well.
 */
public class ClickableArrowKeyMovementMethod extends ArrowKeyMovementMethod {

	private static ClickableArrowKeyMovementMethod instance;

	private final RectF touchedLineBounds = new RectF();

	public static MovementMethod getInstance() {
		if (instance == null) {
			instance = new ClickableArrowKeyMovementMethod();
		}
		return instance;
	}

	@Override
	public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
		// If action has finished
		if (event.getAction() == MotionEvent.ACTION_UP) {
			// Locate the area that was pressed
			int x = (int) event.getX();
			int y = (int) event.getY();
			x -= widget.getTotalPaddingLeft();
			y -= widget.getTotalPaddingTop();
			x += widget.getScrollX();
			y += widget.getScrollY();

			// Locate the text line
			Layout layout = widget.getLayout();
			int line = layout.getLineForVertical(y);

			// Check that the touch actually happened within the line bounds
			touchedLineBounds.left = layout.getLineLeft(line);
			touchedLineBounds.top = layout.getLineTop(line);
			touchedLineBounds.right = layout.getLineWidth(line) + touchedLineBounds.left;
			touchedLineBounds.bottom = layout.getLineBottom(line);

			if (touchedLineBounds.contains(x, y)) {
				int offset = layout.getOffsetForHorizontal(line, x);
				// Find a clickable span at that text offset, if any
				ClickableSpan[] link = buffer.getSpans(offset, offset, ClickableSpan.class);
				if (link.length > 0) {
					link[0].onClick(widget);
					return true;
				}
			}
		}

		return super.onTouchEvent(widget, buffer, event);
	}
}
