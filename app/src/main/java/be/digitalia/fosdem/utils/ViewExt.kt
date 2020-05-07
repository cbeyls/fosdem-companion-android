package be.digitalia.fosdem.utils

import android.app.Activity
import android.content.ContextWrapper
import android.view.View

fun View.findActivity(): Activity {
    var context = this.context
    while (context !is Activity) {
        context = if (context is ContextWrapper) {
            context.baseContext
        } else {
            throw IllegalStateException("Context is not an Activity")
        }
    }
    return context
}