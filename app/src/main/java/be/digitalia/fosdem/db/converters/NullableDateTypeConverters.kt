package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import java.util.Date

object NullableDateTypeConverters {
    @JvmStatic
    @TypeConverter
    fun toDate(value: Long?): Date? = value?.let { Date(it) }

    @JvmStatic
    @TypeConverter
    fun fromDate(value: Date?): Long? = value?.time
}