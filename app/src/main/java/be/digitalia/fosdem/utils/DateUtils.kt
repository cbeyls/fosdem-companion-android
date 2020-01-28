package be.digitalia.fosdem.utils

import android.content.Context
import java.text.DateFormat
import java.util.*

object DateUtils {
    val belgiumTimeZone: TimeZone = TimeZone.getTimeZone("GMT+1")

    fun DateFormat.withBelgiumTimeZone(): DateFormat {
        timeZone = belgiumTimeZone
        return this
    }

    fun getTimeDateFormat(context: Context): DateFormat {
        return android.text.format.DateFormat.getTimeFormat(context).withBelgiumTimeZone()
    }

    fun getYear(timestamp: Long, calendar: Calendar? = null): Int {
        val cal = calendar ?: Calendar.getInstance(belgiumTimeZone, Locale.US)
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR)
    }
}