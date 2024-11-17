package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import java.time.Instant

object NonNullInstantTypeConverters {
    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochSecond(value)

    @TypeConverter
    fun fromInstant(value: Instant): Long = value.epochSecond
}