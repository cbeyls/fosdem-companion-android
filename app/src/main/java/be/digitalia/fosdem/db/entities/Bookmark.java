package be.digitalia.fosdem.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = Bookmark.TABLE_NAME)
public class Bookmark {

	public static final String TABLE_NAME = "bookmarks";

	@PrimaryKey
	@ColumnInfo(name = "event_id")
	private final long eventId;

	public Bookmark(long eventId) {
		this.eventId = eventId;
	}

	public long getEventId() {
		return eventId;
	}
}
