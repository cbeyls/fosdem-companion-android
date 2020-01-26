package be.digitalia.fosdem.utils

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun DrawerLayout.awaitCloseDrawer(drawerView: View) = suspendCancellableCoroutine<Unit> { cont ->
    val listener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerStateChanged(newState: Int) {
            if (newState == DrawerLayout.STATE_DRAGGING) {
                cont.cancel()
            }
        }

        override fun onDrawerClosed(drawerView: View) {
            removeDrawerListener(this)
            cont.resume(Unit)
        }
    }
    cont.invokeOnCancellation {
        removeDrawerListener(listener)
    }
    addDrawerListener(listener)
    closeDrawer(drawerView)
}