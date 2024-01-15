package be.digitalia.fosdem.utils

import android.content.Context
import android.text.format.DateFormat
import androidx.core.os.ConfigurationCompat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateUtils {
    fun getTimeFormatter(context: Context): DateTimeFormatter {
        val primaryLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
        val basePattern = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        val bestPattern = DateFormat.getBestDateTimePattern(primaryLocale, basePattern)
        return DateTimeFormatter.ofPattern(bestPattern, primaryLocale)
    }
}

fun Instant.toLocalDateTime(zoneId: ZoneId): LocalDateTime {
    return LocalDateTime.ofInstant(this, zoneId)
}

/**
 * Convert to Instant without allocating intermediate objects if possible.
 */
fun LocalDateTime.toInstant(zoneId: ZoneId): Instant {
    val zoneOffset = if (zoneId is ZoneOffset) zoneId else zoneId.rules.getOffset(this)
    return toInstant(zoneOffset)
}