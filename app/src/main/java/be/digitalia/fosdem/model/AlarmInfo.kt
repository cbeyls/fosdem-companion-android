package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NullableDateTypeConverters
import be.digitalia.fosdem.utils.DateParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import java.util.Date

@Parcelize
data class AlarmInfo(
        @ColumnInfo(name = "event_id")
        val eventId: Long,
        @ColumnInfo(name = "start_time")
        @field:TypeConverters(NullableDateTypeConverters::class)
        val startTime: @WriteWith<DateParceler> Date?
) : Parcelable