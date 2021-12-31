package be.digitalia.fosdem.utils

import android.content.Context
import android.text.format.DateFormat
import androidx.core.os.ConfigurationCompat
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateUtils {
    val conferenceZoneId: ZoneId = ZoneOffset.ofHours(1)

    fun getTimeFormatter(context: Context): DateTimeFormatter {
        val primaryLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
        val basePattern = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        val bestPattern = DateFormat.getBestDateTimePattern(primaryLocale, basePattern)
        return DateTimeFormatter.ofPattern(bestPattern, primaryLocale)
    }
}