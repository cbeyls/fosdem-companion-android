package be.digitalia.fosdem.db;

import java.util.Date;

import androidx.room.TypeConverter;
import be.digitalia.fosdem.model.Track;

public class Converters {
	@TypeConverter
	public static Date toDate(Long value) {
		return value == null ? null : new Date(value);
	}

	@TypeConverter
	public static Long fromDate(Date value) {
		return (value == null) ? null : value.getTime();
	}

	@TypeConverter
	public static Track.Type toTrackType(String value) {
		return Track.Type.valueOf(value);
	}

	@TypeConverter
	public static String fromTrackType(Track.Type value) {
		return value.name();
	}
}
