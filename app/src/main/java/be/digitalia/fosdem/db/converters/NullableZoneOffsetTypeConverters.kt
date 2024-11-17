package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import java.time.ZoneOffset

object NullableZoneOffsetTypeConverters {
    @TypeConverter
    fun toZoneOffset(value: Int?): ZoneOffset? = value?.let { ZoneOffset.ofTotalSeconds(it) }

    @TypeConverter
    fun fromZoneOffset(value: ZoneOffset?): Int? = value?.totalSeconds
}