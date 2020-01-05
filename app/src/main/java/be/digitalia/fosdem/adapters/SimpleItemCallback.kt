package be.digitalia.fosdem.adapters

import androidx.recyclerview.widget.DiffUtil

/**
 * Implementation of DiffUtil.ItemCallback which uses Object.equals() to determine if items are the same.
 */
@Deprecated("Natural key should be used in a custom areItemsTheSame() implementation")
abstract class SimpleItemCallback<T> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem == newItem
}