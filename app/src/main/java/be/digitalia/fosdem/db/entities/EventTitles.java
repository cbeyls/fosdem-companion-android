package be.digitalia.fosdem.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts3;
import androidx.room.PrimaryKey;
import be.digitalia.fosdem.model.Event;

@Fts3
@Entity(tableName = EventTitles.TABLE_NAME)
public class EventTitles {

	public static final String TABLE_NAME = "events_titles";

	@PrimaryKey
	@ColumnInfo(name = "rowid")
	private final long id;
	private final String title;
	@ColumnInfo(name = "subtitle")
	private final String subTitle;

	public EventTitles(Event event) {
		this(event.getId(), event.getTitle(), event.getSubTitle());
	}

	public EventTitles(long id, String title, String subTitle) {
		this.id = id;
		this.title = title;
		this.subTitle = subTitle;
	}

	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getSubTitle() {
		return subTitle;
	}
}
