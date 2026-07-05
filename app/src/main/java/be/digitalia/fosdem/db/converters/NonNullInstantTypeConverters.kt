package be.digitalia.fosdem.db.converters

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant

object NonNullInstantTypeConverters {
    @ColumnTypeConverter
    fun toInstant(value: Long): Instant = Instant.fromEpochSeconds(value)

    @ColumnTypeConverter
    fun fromInstant(value: Instant): Long = value.epochSeconds
}