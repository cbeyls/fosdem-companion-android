package be.digitalia.fosdem.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import be.digitalia.fosdem.R
import kotlin.math.roundToInt

class MaterialHorizontalProgressBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.materialHorizontalProgressBarStyle)
    : ProgressBar(context, attrs, defStyleAttr) {

    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            progressDrawable = createProgressDrawable()
            indeterminateDrawable = AppCompatResources.getDrawable(context, R.drawable.avd_progress_indeterminate_horizontal)
        }
    }

    @SuppressLint("ResourceType")
    private fun createProgressDrawable(): Drawable? {
        return (ContextCompat.getDrawable(context, R.drawable.progress_horizontal_material) as? LayerDrawable)?.apply {
            val a = context.theme.obtainStyledAttributes(intArrayOf(R.attr.colorControlNormal, R.attr.colorControlActivated, android.R.attr.disabledAlpha))
            val colorControlNormal = a.getColor(0, Color.TRANSPARENT)
            val colorControlActivated = a.getColor(1, Color.TRANSPARENT)
            val disabledAlpha = (a.getFloat(2, 0f) * 255f).roundToInt().coerceIn(0, 255)
            a.recycle()

            with(findDrawableByLayerId(android.R.id.background)) {
                mutate()
                alpha = disabledAlpha
                colorFilter = PorterDuffColorFilter(colorControlNormal, PorterDuff.Mode.SRC_IN)
            }
            with(findDrawableByLayerId(android.R.id.secondaryProgress)) {
                mutate()
                alpha = disabledAlpha
                colorFilter = PorterDuffColorFilter(colorControlActivated, PorterDuff.Mode.SRC_IN)
            }
            with(findDrawableByLayerId(android.R.id.progress)) {
                mutate()
                colorFilter = PorterDuffColorFilter(colorControlActivated, PorterDuff.Mode.SRC_IN)
            }
        }
    }
}