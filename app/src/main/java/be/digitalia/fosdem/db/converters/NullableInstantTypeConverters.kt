package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import java.time.Instant

object NullableInstantTypeConverters {
    @JvmStatic
    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochSecond(it) }

    @JvmStatic
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.epochSecond
}