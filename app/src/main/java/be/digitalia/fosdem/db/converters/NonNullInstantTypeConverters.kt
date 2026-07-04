package be.digitalia.fosdem.db.converters

import androidx.room3.ColumnTypeConverter
import java.time.Instant

object NonNullInstantTypeConverters {
    @ColumnTypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochSecond(value)

    @ColumnTypeConverter
    fun fromInstant(value: Instant): Long = value.epochSecond
}