package be.digitalia.fosdem.utils

import android.content.Context
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    val belgiumTimeZone: TimeZone = TimeZone.getTimeZone("GMT+1")

    fun DateFormat.withBelgiumTimeZone(): DateFormat {
        timeZone = belgiumTimeZone
        return this
    }

    fun getTimeDateFormat(context: Context): DateFormat {
        return android.text.format.DateFormat.getTimeFormat(context).withBelgiumTimeZone()
    }

    fun getYear(timestamp: Long, calendar: Calendar = Calendar.getInstance(belgiumTimeZone, Locale.US)): Int {
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.YEAR)
    }
}