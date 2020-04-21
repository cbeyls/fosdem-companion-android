package be.digitalia.fosdem.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.MenuItem

// Workaround for disappearing menu items bug
fun MenuItem.fixCollapsibleActionView() {
    setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
        override fun onMenuItemActionExpand(item: MenuItem): Boolean {
            return true
        }

        override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
            item.actionView?.context?.findActivity()?.invalidateOptionsMenu()
            return true
        }
    })
}

private tailrec fun Context.findActivity(): Activity? {
    if (this is Activity) {
        return this
    }
    if (this !is ContextWrapper) {
        return null
    }
    return baseContext.findActivity()
}