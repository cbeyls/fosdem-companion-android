package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import java.time.LocalDate

object NonNullLocalDateTypeConverters {
    @TypeConverter
    fun toLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate): Long = value.toEpochDay()
}