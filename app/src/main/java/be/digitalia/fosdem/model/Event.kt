package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.TypeConverters
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.db.converters.NullableDateTypeConverters
import be.digitalia.fosdem.utils.DateParceler
import be.digitalia.fosdem.utils.DateUtils
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import java.util.Date

@Parcelize
data class Event(
        val id: Long,
        @Embedded(prefix = "day_")
        val day: Day,
        @ColumnInfo(name = "start_time")
        @field:TypeConverters(NullableDateTypeConverters::class)
        val startTime: @WriteWith<DateParceler> Date? = null,
        @ColumnInfo(name = "end_time")
        @field:TypeConverters(NullableDateTypeConverters::class)
        val endTime: @WriteWith<DateParceler> Date? = null,
        @ColumnInfo(name = "room_name")
        val roomName: String?,
        val slug: String?,
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

    fun isRunningAtTime(time: Long): Boolean {
        return startTime != null && endTime != null && time in startTime.time..endTime.time
    }

    /**
     * @return The event duration in minutes
     */
    val duration: Int
        get() = if (startTime == null || endTime == null) {
            0
        } else ((endTime.time - startTime.time) / android.text.format.DateUtils.MINUTE_IN_MILLIS).toInt()

    val url: String?
        get() {
            val s = slug ?: return null
            return FosdemUrls.getEvent(s, DateUtils.getYear(day.date.time))
        }

    override fun toString(): String = title.orEmpty()
}