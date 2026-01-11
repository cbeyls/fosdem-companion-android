package be.digitalia.fosdem.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

val AppCompatActivity.rootView: View
    get() = findViewById(android.R.id.content)

fun View.consumeHorizontalWindowInsetsAsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val padding =
            insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        v.setPadding(padding.left, 0, padding.right, 0)
        insets.inset(padding.left, 0, padding.right, 0)
    }
}