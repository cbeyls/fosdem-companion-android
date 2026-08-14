package be.digitalia.fosdem.db.converters

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant

object NullableInstantTypeConverters {
    @ColumnTypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.fromEpochSeconds(it) }

    @ColumnTypeConverter
    fun fromInstant(value: Instant?): Long? = value?.epochSeconds
}