package be.digitalia.fosdem.livedata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Encapsulates data that can only be consumed once.
 */
public class SingleEvent<T> {

	private T content;

	public SingleEvent(@NonNull T content) {
		this.content = content;
	}

	/**
	 * @return The content, or null if it has already been consumed.
	 */
	@Nullable
	public T consume() {
		final T previousContent = content;
		if (previousContent != null) {
			content = null;
		}
		return previousContent;
	}
}
