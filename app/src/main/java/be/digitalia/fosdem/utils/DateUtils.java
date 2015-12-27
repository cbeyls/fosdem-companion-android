package be.digitalia.fosdem.utils;

import android.content.Context;

import java.text.DateFormat;
import java.util.TimeZone;

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
}
