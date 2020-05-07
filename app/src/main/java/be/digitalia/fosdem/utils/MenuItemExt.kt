package be.digitalia.fosdem.utils

import android.view.MenuItem

// Workaround for disappearing menu items bug
fun MenuItem.fixCollapsibleActionView() {
    setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
        override fun onMenuItemActionExpand(item: MenuItem): Boolean {
            return true
        }

        override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
            item.actionView?.findActivity()?.invalidateOptionsMenu()
            return true
        }
    })
}