package be.digitalia.fosdem.db.converters

import androidx.room3.ColumnTypeConverter
import java.time.Instant

object NullableInstantTypeConverters {
    @ColumnTypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochSecond(it) }

    @ColumnTypeConverter
    fun fromInstant(value: Instant?): Long? = value?.epochSecond
}