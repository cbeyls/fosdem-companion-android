package be.digitalia.fosdem.db.converters

import androidx.room3.ColumnTypeConverter
import java.time.ZoneOffset

object NullableZoneOffsetTypeConverters {
    @ColumnTypeConverter
    fun toZoneOffset(value: Int?): ZoneOffset? = value?.let { ZoneOffset.ofTotalSeconds(it) }

    @ColumnTypeConverter
    fun fromZoneOffset(value: ZoneOffset?): Int? = value?.totalSeconds
}