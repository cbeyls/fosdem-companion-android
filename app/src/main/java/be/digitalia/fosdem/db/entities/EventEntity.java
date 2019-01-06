package be.digitalia.fosdem.db.entities;

import java.util.Date;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import be.digitalia.fosdem.db.converters.NullableDateTypeConverters;

@Entity(tableName = EventEntity.TABLE_NAME, indices = {
		@Index(value = {"day_index"}, name = "event_day_index_idx"),
		@Index(value = {"start_time"}, name = "event_start_time_idx"),
		@Index(value = {"end_time"}, name = "event_end_time_idx"),
		@Index(value = {"track_id"}, name = "event_track_id_idx")
})
public class EventEntity {

	public static final String TABLE_NAME = "events";

	@PrimaryKey
	private long id;
	@ColumnInfo(name = "day_index")
	private int dayIndex;
	@ColumnInfo(name = "start_time")
	@TypeConverters({NullableDateTypeConverters.class})
	private Date startTime;
	@ColumnInfo(name = "end_time")
	@TypeConverters({NullableDateTypeConverters.class})
	private Date endTime;
	@ColumnInfo(name = "room_name")
	private String roomName;
	private String slug;
	@ColumnInfo(name = "track_id")
	private Long trackId;
	@ColumnInfo(name = "abstract")
	private String abstractText;
	private String description;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getDayIndex() {
		return dayIndex;
	}

	public void setDayIndex(int dayIndex) {
		this.dayIndex = dayIndex;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public Long getTrackId() {
		return trackId;
	}

	public void setTrackId(Long trackId) {
		this.trackId = trackId;
	}

	public String getAbstractText() {
		return abstractText;
	}

	public void setAbstractText(String abstractText) {
		this.abstractText = abstractText;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
