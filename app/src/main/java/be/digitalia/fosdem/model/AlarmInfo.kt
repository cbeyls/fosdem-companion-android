package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NullableInstantTypeConverters
import be.digitalia.fosdem.utils.InstantParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import java.time.Instant

@Parcelize
data class AlarmInfo(
        @ColumnInfo(name = "event_id")
        val eventId: Long,
        @ColumnInfo(name = "start_time")
        @field:TypeConverters(NullableInstantTypeConverters::class)
        val startTime: @WriteWith<InstantParceler> Instant?
) : Parcelable