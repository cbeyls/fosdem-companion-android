package be.digitalia.fosdem.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NullableInstantTypeConverters
import be.digitalia.fosdem.db.converters.NullableZoneOffsetTypeConverters
import java.time.Instant
import java.time.ZoneOffset

@Entity(tableName = EventEntity.TABLE_NAME, indices = [
    Index(value = ["day_index"], name = "event_day_index_idx"),
    Index(value = ["start_time"], name = "event_start_time_idx"),
    Index(value = ["end_time"], name = "event_end_time_idx"),
    Index(value = ["track_id"], name = "event_track_id_idx")
])
class EventEntity(
        @PrimaryKey
        val id: Long,
        @ColumnInfo(name = "day_index")
        val dayIndex: Int,
        @ColumnInfo(name = "start_time")
        @field:TypeConverters(NullableInstantTypeConverters::class)
        val startTime: Instant?,
        @ColumnInfo(name = "start_time_offset")
        @field:TypeConverters(NullableZoneOffsetTypeConverters::class)
        val startTimeOffset: ZoneOffset?,
        @ColumnInfo(name = "end_time")
        @field:TypeConverters(NullableInstantTypeConverters::class)
        val endTime: Instant?,
        @ColumnInfo(name = "room_name")
        val roomName: String?,
        val url: String?,
        @ColumnInfo(name = "track_id")
        val trackId: Long,
        @ColumnInfo(name = "abstract")
        val abstractText: String?,
        val description: String?
) {
    companion object {
        const val TABLE_NAME = "events"
    }
}