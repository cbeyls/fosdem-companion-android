package be.digitalia.fosdem.model

import android.os.Parcelable
import android.text.format.DateUtils
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.TypeConverters
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.db.converters.NullableDateTypeConverters
import be.digitalia.fosdem.utils.DateUtils.getYear
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Event(
        val id: Long,
        @Embedded(prefix = "day_")
        val day: Day,
        @ColumnInfo(name = "start_time")
        @field:TypeConverters(NullableDateTypeConverters::class)
        val startTime: Date? = null,
        @ColumnInfo(name = "end_time")
        @field:TypeConverters(NullableDateTypeConverters::class)
        val endTime: Date? = null,
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
        } else ((endTime.time - startTime.time) / DateUtils.MINUTE_IN_MILLIS).toInt()

    val url: String?
        get() {
            val s = slug ?: return null
            return FosdemUrls.getEvent(s, getYear(day.date.time))
        }

    override fun toString(): String = title ?: ""
}