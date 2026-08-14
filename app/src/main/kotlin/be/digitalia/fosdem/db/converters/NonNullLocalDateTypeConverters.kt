package be.digitalia.fosdem.db.converters

import androidx.room3.ColumnTypeConverter
import java.time.LocalDate

object NonNullLocalDateTypeConverters {
    @ColumnTypeConverter
    fun toLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @ColumnTypeConverter
    fun fromLocalDate(value: LocalDate): Long = value.toEpochDay()
}