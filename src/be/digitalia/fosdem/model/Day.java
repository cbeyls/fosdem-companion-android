package be.digitalia.fosdem.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;
import be.digitalia.fosdem.utils.DateUtils;

public class Day implements Parcelable {

	private static final DateFormat DAY_DATE_FORMAT = DateUtils.withBelgiumTimeZone(new SimpleDateFormat("EEEE", Locale.US));

	private int index;
	private Date date;

	public Day() {
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getName() {
		return String.format(Locale.US, "Day %1$d (%2$s)", index, DAY_DATE_FORMAT.format(date));
	}

	public String getShortName() {
		return DAY_DATE_FORMAT.format(date);
	}

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
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(index);
		out.writeLong((date == null) ? 0L : date.getTime());
	}

	public static final Parcelable.Creator<Day> CREATOR = new Parcelable.Creator<Day>() {
		public Day createFromParcel(Parcel in) {
			return new Day(in);
		}

		public Day[] newArray(int size) {
			return new Day[size];
		}
	};

	private Day(Parcel in) {
		index = in.readInt();
		long time = in.readLong();
		if (time != 0L) {
			date = new Date(time);
		}
	}
}
