package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import java.time.Instant

object NonNullInstantTypeConverters {
    @JvmStatic
    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochSecond(value)

    @JvmStatic
    @TypeConverter
    fun fromInstant(value: Instant): Long = value.epochSecond
}