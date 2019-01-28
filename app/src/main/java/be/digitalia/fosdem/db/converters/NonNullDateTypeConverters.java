package be.digitalia.fosdem.db.converters;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

public class NonNullDateTypeConverters {
	@TypeConverter
	public static Date toDate(long value) {
		return new Date(value);
	}

	@TypeConverter
	public static long fromDate(@NonNull Date value) {
		return value.getTime();
	}
}
