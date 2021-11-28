package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import java.time.LocalDate

object NonNullLocalDateTypeConverters {
    @JvmStatic
    @TypeConverter
    fun toLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @JvmStatic
    @TypeConverter
    fun fromLocalDate(value: LocalDate): Long = value.toEpochDay()
}