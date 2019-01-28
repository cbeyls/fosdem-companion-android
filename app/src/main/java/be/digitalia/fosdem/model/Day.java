package be.digitalia.fosdem.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import be.digitalia.fosdem.db.converters.NonNullDateTypeConverters;
import be.digitalia.fosdem.utils.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = Day.TABLE_NAME)
public class Day implements Comparable<Day>, Parcelable {

	public static final String TABLE_NAME = "days";

	private static final DateFormat DAY_DATE_FORMAT = DateUtils.withBelgiumTimeZone(new SimpleDateFormat("EEEE", Locale.US));

	@PrimaryKey
	private int index;
	@TypeConverters({NonNullDateTypeConverters.class})
	@NonNull
	private Date date;

	public Day() {
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@NonNull
	public Date getDate() {
		return date;
	}

	public void setDate(@NonNull Date date) {
		this.date = date;
	}

	public String getName() {
		return String.format(Locale.US, "Day %1$d (%2$s)", index, DAY_DATE_FORMAT.format(date));
	}

	public String getShortName() {
		return DAY_DATE_FORMAT.format(date);
	}

	@NonNull
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Day other = (Day) obj;
		return (index == other.index);
	}

	@Override
	public int compareTo(@NonNull Day other) {
		return index - other.index;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(index);
		out.writeLong(date.getTime());
	}

	public static final Parcelable.Creator<Day> CREATOR = new Parcelable.Creator<Day>() {
		public Day createFromParcel(Parcel in) {
			return new Day(in);
		}

		public Day[] newArray(int size) {
			return new Day[size];
		}
	};

	Day(Parcel in) {
		index = in.readInt();
		date = new Date(in.readLong());
	}
}
