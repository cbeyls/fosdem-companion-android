package be.digitalia.fosdem.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;

/**
 * {@link android.widget.LinearLayout} implementing the {@link android.widget.Checkable}
 * interface by keeping an internal 'checked' state flag.
 */
public class CheckableLinearLayout extends ForegroundLinearLayout implements Checkable {
	private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

	private boolean mChecked = false;

	public CheckableLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public boolean isChecked() {
		return mChecked;
	}

	public void setChecked(boolean b) {
		if (b != mChecked) {
			mChecked = b;
			refreshDrawableState();
		}
	}

	public void toggle() {
		setChecked(!mChecked);
	}

	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}
}