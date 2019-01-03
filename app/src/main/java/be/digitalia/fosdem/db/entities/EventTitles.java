package be.digitalia.fosdem.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts3;
import androidx.room.PrimaryKey;

@Fts3
@Entity(tableName = EventTitles.TABLE_NAME)
public class EventTitles {

	public static final String TABLE_NAME = "events_titles";

	@PrimaryKey
	@ColumnInfo(name = "rowid")
	private long id;
	private String title;
	@ColumnInfo(name = "subtitle")
	private String subTitle;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}
}
