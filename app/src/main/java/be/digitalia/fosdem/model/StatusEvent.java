package be.digitalia.fosdem.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;

public class StatusEvent {

	@Embedded
	@NonNull
	private Event event;
	@ColumnInfo(name = "is_bookmarked")
	private boolean isBookmarked;

	public StatusEvent(@NonNull Event event, boolean isBookmarked) {
		this.event = event;
		this.isBookmarked = isBookmarked;
	}

	@NonNull
	public Event getEvent() {
		return event;
	}

	public boolean isBookmarked() {
		return isBookmarked;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		StatusEvent other = (StatusEvent) obj;
		return event.equals(other.event);
	}
}
