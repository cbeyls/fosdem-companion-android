package be.digitalia.fosdem.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = EventToPerson.TABLE_NAME, primaryKeys = ["event_id", "person_id"],
        indices = [Index(value = ["person_id"], name = "event_person_person_id_idx")])
class EventToPerson(
        @ColumnInfo(name = "event_id")
        val eventId: Long,
        @ColumnInfo(name = "person_id")
        val personId: Long
) {
    companion object {
        const val TABLE_NAME = "events_persons"
    }
}