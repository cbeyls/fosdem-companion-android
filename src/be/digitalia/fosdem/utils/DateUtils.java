package be.digitalia.fosdem.utils;

import java.text.DateFormat;
import java.util.TimeZone;

public class DateUtils {

	private static final TimeZone belgiumTimeZone = TimeZone.getTimeZone("GMT+1");

	public static TimeZone getBelgiumTimeZone() {
		return belgiumTimeZone;
	}

	public static DateFormat withBelgiumTimeZone(DateFormat format) {
		format.setTimeZone(belgiumTimeZone);
		return format;
	}
}
