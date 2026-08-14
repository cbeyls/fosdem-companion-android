package be.digitalia.fosdem.widgets

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.core.view.doOnLayout
import androidx.customview.view.AbsSavedState
import com.github.chrisbanes.photoview.PhotoView

/**
 * PhotoView which saves and restores the current scale and approximate position.
 */
class SaveStatePhotoView : PhotoView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet?) : super(context, attr)
    constructor(context: Context, attr: AttributeSet?, defStyle: Int) : super(context, attr, defStyle)

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val rect = displayRect
        val overflowWidth = rect.width() - width
        val pivotX = if (overflowWidth > 0f) {
            -rect.left / overflowWidth
        } else 0.5f
        val overflowHeight = rect.height() - height
        val pivotY = if (overflowHeight > 0f) {
            -rect.top / overflowHeight
        } else 0.5f
        return SavedState(superState ?: AbsSavedState.EMPTY_STATE, scale, pivotX, pivotY)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        doOnLayout {
            setScale(state.scale.coerceIn(minimumScale, maximumScale),
                    width * state.pivotX,
                    height * state.pivotY,
                    false)
        }
    }

    class SavedState : AbsSavedState {

        val scale: Float
        val pivotX: Float
        val pivotY: Float

        constructor(superState: Parcelable, scale: Float, pivotX: Float, pivotY: Float) : super(superState) {
            this.scale = scale
            this.pivotX = pivotX
            this.pivotY = pivotY
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(scale)
            out.writeFloat(pivotX)
            out.writeFloat(pivotY)
        }

        private constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            scale = source.readFloat()
            pivotX = source.readFloat()
            pivotY = source.readFloat()
        }

        companion object {
            @JvmField
            @Suppress("UNUSED")
            val CREATOR = object : Parcelable.ClassLoaderCreator<SavedState> {
                override fun createFromParcel(source: Parcel, loader: ClassLoader?) = SavedState(source, loader)

                override fun createFromParcel(source: Parcel) = SavedState(source, null)

                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }
    }
}