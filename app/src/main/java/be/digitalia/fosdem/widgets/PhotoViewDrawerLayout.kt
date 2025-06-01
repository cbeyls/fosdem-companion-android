package be.digitalia.fosdem.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.drawerlayout.widget.DrawerLayout

/**
 * DrawerLayout which includes a fix to prevent crashes with PhotoView.
 *
 *
 * See https://github.com/chrisbanes/PhotoView#issues-with-viewgroups
 * http://code.google.com/p/android/issues/detail?id=18990
 */
class PhotoViewDrawerLayout : DrawerLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (_: Exception) {
            false
        }
    }
}