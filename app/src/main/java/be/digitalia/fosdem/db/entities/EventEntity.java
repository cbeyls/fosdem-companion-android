package be.digitalia.fosdem.db.entities;

import java.util.Date;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import be.digitalia.fosdem.db.converters.NullableDateTypeConverters;
import be.digitalia.fosdem.model.Event;

@Entity(tableName = EventEntity.TABLE_NAME, indices = {
		@Index(value = {"day_index"}, name = "event_day_index_idx"),
		@Index(value = {"start_time"}, name = "event_start_time_idx"),
		@Index(value = {"end_time"}, name = "event_end_time_idx"),
		@Index(value = {"track_id"}, name = "event_track_id_idx")
})
public class EventEntity {

	public static final String TABLE_NAME = "events";

	@PrimaryKey
	private final long id;
	@ColumnInfo(name = "day_index")
	private final int dayIndex;
	@ColumnInfo(name = "start_time")
	@TypeConverters({NullableDateTypeConverters.class})
	private final Date startTime;
	@ColumnInfo(name = "end_time")
	@TypeConverters({NullableDateTypeConverters.class})
	private final Date endTime;
	@ColumnInfo(name = "room_name")
	private final String roomName;
	private final String slug;
	@ColumnInfo(name = "track_id")
	private final long trackId;
	@ColumnInfo(name = "abstract")
	private final String abstractText;
	private final String description;

	public EventEntity(Event event) {
		this(event.getId(), event.getDay().getIndex(), event.getStartTime(), event.getEndTime(), event.getRoomName(),
				event.getSlug(), event.getTrack().getId(), event.getAbstractText(), event.getDescription());
	}

	public EventEntity(long id, int dayIndex, Date startTime, Date endTime, String roomName,
					   String slug, long trackId, String abstractText, String description) {
		this.id = id;
		this.dayIndex = dayIndex;
		this.startTime = startTime;
		this.endTime = endTime;
		this.roomName = roomName;
		this.slug = slug;
		this.trackId = trackId;
		this.abstractText = abstractText;
		this.description = description;
	}

	public long getId() {
		return id;
	}

	public int getDayIndex() {
		return dayIndex;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getRoomName() {
		return roomName;
	}

	public String getSlug() {
		return slug;
	}

	public long getTrackId() {
		return trackId;
	}

	public String getAbstractText() {
		return abstractText;
	}

	public String getDescription() {
		return description;
	}
}
