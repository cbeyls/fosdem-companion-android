package be.digitalia.fosdem.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Date;
import java.util.List;

import be.digitalia.fosdem.api.FosdemUrls;
import be.digitalia.fosdem.utils.DateUtils;

public class Event implements Parcelable {

	private long id;
	private Day day;
	private Date startTime;
	private Date endTime;
	private String roomName;
	private String slug;
	private String title;
	private String subTitle;
	private Track track;
	private String abstractText;
	private String description;
	private String personsSummary;
	private List<Person> persons; // Optional
	private List<Link> links; // Optional

	public Event() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Day getDay() {
		return day;
	}

	public void setDay(Day day) {
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

	public Track getTrack() {
		return track;
	}

	public void setTrack(Track track) {
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

	public String getPersonsSummary() {
		if (personsSummary != null) {
			return personsSummary;
		}
		if (persons != null) {
			return TextUtils.join(", ", persons);
		}
		return "";
	}

	public void setPersonsSummary(String personsSummary) {
		this.personsSummary = personsSummary;
	}

	public List<Person> getPersons() {
		return persons;
	}

	public void setPersons(List<Person> persons) {
		this.persons = persons;
	}

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}

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
		out.writeTypedList(persons);
		out.writeTypedList(links);
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
		persons = in.createTypedArrayList(Person.CREATOR);
		links = in.createTypedArrayList(Link.CREATOR);
	}
}
