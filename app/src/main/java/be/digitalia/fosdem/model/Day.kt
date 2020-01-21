package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import be.digitalia.fosdem.db.converters.NonNullDateTypeConverters
import be.digitalia.fosdem.utils.DateParceler
import be.digitalia.fosdem.utils.DateUtils.withBelgiumTimeZone
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = Day.TABLE_NAME)
@Parcelize
data class Day(
        @PrimaryKey
        val index: Int,
        @field:TypeConverters(NonNullDateTypeConverters::class)
        val date: @WriteWith<DateParceler> Date
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

        private val DAY_DATE_FORMAT = SimpleDateFormat("EEEE", Locale.US).withBelgiumTimeZone()
    }
}