package be.digitalia.fosdem.model

import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NullableDateTypeConverters
import java.util.*

class AlarmInfo(
        @ColumnInfo(name = "event_id")
        val eventId: Long,
        @ColumnInfo(name = "start_time")
        @field:TypeConverters(NullableDateTypeConverters::class)
        val startTime: Date?
)