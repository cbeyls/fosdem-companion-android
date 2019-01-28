package be.digitalia.fosdem.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.TypeConverters;
import be.digitalia.fosdem.api.FosdemUrls;
import be.digitalia.fosdem.db.converters.NullableDateTypeConverters;
import be.digitalia.fosdem.utils.DateUtils;

import java.util.Date;

public class Event implements Parcelable {

	private long id;
	@Embedded(prefix = "day_")
	@NonNull
	private Day day;
	@ColumnInfo(name = "start_time")
	@TypeConverters({NullableDateTypeConverters.class})
	private Date startTime;
	@ColumnInfo(name = "end_time")
	@TypeConverters({NullableDateTypeConverters.class})
	private Date endTime;
	@ColumnInfo(name = "room_name")
	private String roomName;
	private String slug;
	private String title;
	@ColumnInfo(name = "subtitle")
	private String subTitle;
	@Embedded(prefix = "track_")
	@NonNull
	private Track track;
	@ColumnInfo(name = "abstract")
	private String abstractText;
	private String description;
	@ColumnInfo(name = "persons")
	private String personsSummary;

	public Event() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@NonNull
	public Day getDay() {
		return day;
	}

	public void setDay(@NonNull Day day) {
		this.day = day;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public boolean isRunningAtTime(long time) {
		return (startTime != null) && (endTime != null) && (startTime.getTime() < time) && (time < endTime.getTime());
	}

	/**
	 * @return The event duration in minutes
	 */
	public int getDuration() {
		if ((startTime == null) || (endTime == null)) {
			return 0;
		}
		return (int) ((this.endTime.getTime() - this.startTime.getTime()) / android.text.format.DateUtils.MINUTE_IN_MILLIS);
	}

	public String getRoomName() {
		return (roomName == null) ? "" : roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getUrl() {
		return FosdemUrls.getEvent(slug, DateUtils.getYear(getDay().getDate().getTime()));
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}

	@NonNull
	public Track getTrack() {
		return track;
	}

	public void setTrack(@NonNull Track track) {
		this.track = track;
	}

	public String getAbstractText() {
		return abstractText;
	}

	public void setAbstractText(String abstractText) {
		this.abstractText = abstractText;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@NonNull
	public String getPersonsSummary() {
		if (personsSummary != null) {
			return personsSummary;
		}
		return "";
	}

	public void setPersonsSummary(String personsSummary) {
		this.personsSummary = personsSummary;
	}

	@NonNull
	@Override
	public String toString() {
		return title;
	}

	@Override
	public int hashCode() {
		return (int) (id ^ (id >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Event other = (Event) obj;
		return id == other.id;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(id);
		day.writeToParcel(out, flags);
		out.writeLong((startTime == null) ? 0L : startTime.getTime());
		out.writeLong((endTime == null) ? 0L : endTime.getTime());
		out.writeString(roomName);
		out.writeString(slug);
		out.writeString(title);
		out.writeString(subTitle);
		track.writeToParcel(out, flags);
		out.writeString(abstractText);
		out.writeString(description);
		out.writeString(personsSummary);
	}

	public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
		public Event createFromParcel(Parcel in) {
			return new Event(in);
		}

		public Event[] newArray(int size) {
			return new Event[size];
		}
	};

	Event(Parcel in) {
		id = in.readLong();
		day = Day.CREATOR.createFromParcel(in);
		long time = in.readLong();
		if (time != 0L) {
			startTime = new Date(time);
		}
		time = in.readLong();
		if (time != 0L) {
			endTime = new Date(time);
		}
		roomName = in.readString();
		slug = in.readString();
		title = in.readString();
		subTitle = in.readString();
		track = Track.CREATOR.createFromParcel(in);
		abstractText = in.readString();
		description = in.readString();
		personsSummary = in.readString();
	}
}
