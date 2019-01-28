package be.digitalia.fosdem.adapters;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Implementation of DiffUtil.ItemCallback which uses Object.equals() to determine if items are the same.
 */
public abstract class SimpleItemCallback<T> extends DiffUtil.ItemCallback<T> {
	@Override
	public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
		return ObjectsCompat.equals(oldItem, newItem);
	}
}
