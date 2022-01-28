package be.digitalia.fosdem.utils

import android.app.Activity
import android.app.ActivityManager.TaskDescription
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

var Window.statusBarColorCompat: Int
    @ColorInt
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) statusBarColor else Color.BLACK
    }
    set(@ColorInt color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            statusBarColor = color
        }
    }

@Suppress("DEPRECATION")
fun Activity.setTaskColorPrimary(@ColorInt colorPrimary: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val taskDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TaskDescription(null, 0, colorPrimary or -0x1000000)
        } else {
            TaskDescription(null, null, colorPrimary or -0x1000000)
        }
        setTaskDescription(taskDescription)
    }
}

fun View.tintBackground(backgroundColor: ColorStateList?) {
    background?.let {
        it.mutate()
        DrawableCompat.setTintList(it, backgroundColor)
    }
}

val Context.isLightTheme: Boolean
    get() {
        val value = TypedValue()
        return theme.resolveAttribute(androidx.appcompat.R.attr.isLightTheme, value, true) && value.data != 0
    }

fun ImageView.invertImageColors() {
    val invertColorFilter: ColorFilter = ColorMatrixColorFilter(floatArrayOf(
            -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
            0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
            0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
    ))
    colorFilter = invertColorFilter
}