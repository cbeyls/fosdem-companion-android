package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NullableInstantTypeConverters
import be.digitalia.fosdem.db.converters.NullableZoneOffsetTypeConverters
import be.digitalia.fosdem.utils.InstantParceler
import be.digitalia.fosdem.utils.ZoneOffsetParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@DatabaseView(
    value = """SELECT e.id, e.start_time, e.start_time_offset, e.end_time, e.room_name, e.url,
        et.title, et.subtitle, e.abstract, e.description, e.feedback_url, GROUP_CONCAT(p.name, ', ') AS persons,
        e.day_index, d.date AS day_date, d.start_time AS day_start_time, d.end_time AS day_end_time,
        e.track_id, t.name AS track_name, t.type AS track_type
        FROM events e
        JOIN events_titles et ON e.id = et.`rowid`
        JOIN days d ON e.day_index = d.`index`
        JOIN tracks t ON e.track_id = t.id
        LEFT JOIN events_persons ep ON e.id = ep.event_id
        LEFT JOIN persons p ON ep.person_id = p.`rowid`
        GROUP BY e.id""",
    viewName = "events_view"
)
@Parcelize
data class Event(
        val id: Long,
        @Embedded(prefix = "day_")
        val day: Day,
        @ColumnInfo(name = "start_time")
        @field:TypeConverters(NullableInstantTypeConverters::class)
        val startTime: @WriteWith<InstantParceler> Instant? = null,
        @ColumnInfo(name = "start_time_offset")
        @field:TypeConverters(NullableZoneOffsetTypeConverters::class)
        val startTimeOffset: @WriteWith<ZoneOffsetParceler> ZoneOffset? = null,
        @ColumnInfo(name = "end_time")
        @field:TypeConverters(NullableInstantTypeConverters::class)
        val endTime: @WriteWith<InstantParceler> Instant? = null,
        @ColumnInfo(name = "room_name")
        val roomName: String?,
        val url: String?,
        val title: String?,
        @ColumnInfo(name = "subtitle")
        val subTitle: String?,
        @Embedded(prefix = "track_")
        val track: Track,
        @ColumnInfo(name = "abstract")
        val abstractText: String?,
        val description: String?,
        @ColumnInfo(name = "feedback_url")
        val feedbackUrl: String?,
        @ColumnInfo(name = "persons")
        val personsSummary: String?
) : Parcelable {

    fun startTime(zoneOverride: ZoneId?): LocalDateTime? {
        val zone = zoneOverride ?: startTimeOffset
        return if (startTime != null && zone != null)
            LocalDateTime.ofInstant(startTime, zone)
        else null
    }

    fun endTime(zoneOverride: ZoneId?): LocalDateTime? {
        val zone = zoneOverride ?: startTimeOffset
        return if (endTime != null && zone != null)
            LocalDateTime.ofInstant(endTime, zone)
        else null
    }

    fun isRunningAtTime(time: Instant): Boolean {
        return startTime != null && endTime != null && time >= startTime && time < endTime
    }

    val duration: Duration
        get() = if (startTime == null || endTime == null) {
            Duration.ZERO
        } else {
            Duration.between(startTime, endTime)
        }

    override fun toString(): String = title.orEmpty()
}