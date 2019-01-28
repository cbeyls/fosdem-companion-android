package be.digitalia.fosdem.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(tableName = EventToPerson.TABLE_NAME, primaryKeys = {"event_id", "person_id"},
		indices = {@Index(value = {"person_id"}, name = "event_person_person_id_idx")})
public class EventToPerson {

	public static final String TABLE_NAME = "events_persons";

	@ColumnInfo(name = "event_id")
	private final long eventId;
	@ColumnInfo(name = "person_id")
	private final long personId;

	public EventToPerson(long eventId, long personId) {
		this.eventId = eventId;
		this.personId = personId;
	}

	public long getEventId() {
		return eventId;
	}

	public long getPersonId() {
		return personId;
	}
}
