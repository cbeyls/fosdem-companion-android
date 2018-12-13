package be.digitalia.fosdem.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.drawerlayout.widget.DrawerLayout;

/**
 * DrawerLayout which includes a fix to prevent crashes with PhotoView.
 * <p/>
 * See https://github.com/chrisbanes/PhotoView#issues-with-viewgroups
 * http://code.google.com/p/android/issues/detail?id=18990
 */
public class PhotoViewDrawerLayout extends DrawerLayout {

	public PhotoViewDrawerLayout(Context context) {
		super(context);
	}

	public PhotoViewDrawerLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PhotoViewDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			return super.onInterceptTouchEvent(ev);
		} catch (Exception e) {
			return false;
		}
	}
}
