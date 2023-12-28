package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NonNullInstantTypeConverters
import be.digitalia.fosdem.db.converters.NonNullLocalDateTypeConverters
import be.digitalia.fosdem.utils.InstantParceler
import be.digitalia.fosdem.utils.LocalDateParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(tableName = Day.TABLE_NAME)
@Parcelize
data class Day(
    @PrimaryKey
    val index: Int,
    @field:TypeConverters(NonNullLocalDateTypeConverters::class)
    val date: @WriteWith<LocalDateParceler> LocalDate,
    @ColumnInfo(name = "start_time")
    @field:TypeConverters(NonNullInstantTypeConverters::class)
    val startTime: @WriteWith<InstantParceler> Instant,
    @ColumnInfo(name = "end_time")
    @field:TypeConverters(NonNullInstantTypeConverters::class)
    val endTime: @WriteWith<InstantParceler> Instant
) : Comparable<Day>, Parcelable {

    val name: String
        get() = "Day $index (${DAY_DATE_FORMAT.format(date)})"

    val shortName: String
        get() = DAY_DATE_FORMAT.format(date)

    override fun toString() = name

    override fun compareTo(other: Day): Int {
        return index - other.index
    }

    companion object {
        const val TABLE_NAME = "days"

        private val DAY_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE", Locale.US)
    }
}