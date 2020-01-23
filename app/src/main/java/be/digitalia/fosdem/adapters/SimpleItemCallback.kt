package be.digitalia.fosdem.adapters

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil

/**
 * Creates a DiffUtil.ItemCallback instance using the provided lambda to determine
 * if items are the same and using equals() to determine if item contents are the same.
 */
inline fun <T : Any> createSimpleItemCallback(crossinline areItemsTheSame: (oldItem: T, newItem: T) -> Boolean): DiffUtil.ItemCallback<T> {
    return object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return oldItem == newItem
        }
    }
}