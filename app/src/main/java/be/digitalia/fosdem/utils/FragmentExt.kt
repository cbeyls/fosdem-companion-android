package be.digitalia.fosdem.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * A Lazy implementation which can only be called when a fragment has a view
 * and will automatically clear the value when the view hierarchy gets destroyed.
 */
fun <T : Any> Fragment.viewLifecycleLazy(initializer: () -> T): Lazy<T> = ViewLifecycleLazy(this, initializer)

private class ViewLifecycleLazy<T : Any>(private val fragment: Fragment, private val initializer: () -> T) : Lazy<T>, LifecycleEventObserver {
    private var cached: T? = null

    override val value: T
        get() {
            return cached ?: run {
                val newValue = initializer()
                cached = newValue
                fragment.viewLifecycleOwner.lifecycle.addObserver(this)
                newValue
            }
        }

    override fun isInitialized() = cached != null

    override fun toString() = cached.toString()

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            cached = null
        }
    }
}