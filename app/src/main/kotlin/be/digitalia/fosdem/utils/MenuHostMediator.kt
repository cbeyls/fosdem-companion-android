package be.digitalia.fosdem.utils

import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Allows a single MenuProvider to be registered at any given time,
 * as soon as it reaches the RESUMED state.
 * This allows smoother (non blinking) transition of menu items.
 */
class MenuHostMediator(private val menuHost: MenuHost) {
    private var activeMenuProvider: MenuProvider? = null

    fun addResumedMenuProvider(provider: MenuProvider, owner: LifecycleOwner) {
        owner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // A resumed provider replaces the active one if different
                    activeMenuProvider.let {
                        if (it != provider) {
                            if (it != null) {
                                menuHost.removeMenuProvider(it)
                            }
                            menuHost.addMenuProvider(provider)
                            activeMenuProvider = provider
                        }
                    }
                }

                Lifecycle.Event.ON_DESTROY -> {
                    // Free memory
                    if (activeMenuProvider == provider) {
                        menuHost.removeMenuProvider(provider)
                        activeMenuProvider = null
                    }
                }

                else -> {}
            }
        })
    }
}