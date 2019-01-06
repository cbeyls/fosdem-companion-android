package be.digitalia.fosdem.db.converters;

import java.util.Date;

import androidx.room.TypeConverter;

public class NullableDateTypeConverters {
	@TypeConverter
	public static Date toDate(Long value) {
		return value == null ? null : new Date(value);
	}

	@TypeConverter
	public static Long fromDate(Date value) {
		return (value == null) ? null : value.getTime();
	}
}
