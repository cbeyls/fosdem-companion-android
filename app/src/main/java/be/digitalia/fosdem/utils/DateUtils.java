package be.digitalia.fosdem.utils;

import android.content.Context;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.Nullable;

public class DateUtils {

	private static final TimeZone BELGIUM_TIME_ZONE = TimeZone.getTimeZone("GMT+1");

	public static TimeZone getBelgiumTimeZone() {
		return BELGIUM_TIME_ZONE;
	}

	public static DateFormat withBelgiumTimeZone(DateFormat format) {
		format.setTimeZone(BELGIUM_TIME_ZONE);
		return format;
	}

	public static DateFormat getTimeDateFormat(Context context) {
		return withBelgiumTimeZone(android.text.format.DateFormat.getTimeFormat(context));
	}

	public static int getYear(long timestamp) {
		return getYear(timestamp, null);
	}

	public static int getYear(long timestamp, @Nullable Calendar calendar) {
		if (calendar == null) {
			calendar = Calendar.getInstance(DateUtils.getBelgiumTimeZone(), Locale.US);
		}
		calendar.setTimeInMillis(timestamp);
		return calendar.get(Calendar.YEAR);
	}
}
