package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.ColumnInfo
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