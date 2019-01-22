package be.digitalia.fosdem.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "links", indices = {@Index(value = {"event_id"}, name = "link_event_id_idx")})
public class Link implements Parcelable {

	public static final String TABLE_NAME = "links";

	@PrimaryKey(autoGenerate = true)
	private long id;
	@ColumnInfo(name = "event_id")
	private long eventId;
	@NonNull
	private String url;
	private String description;

	public Link() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getEventId() {
		return eventId;
	}

	public void setEventId(long eventId) {
		this.eventId = eventId;
	}

	@NonNull
	public String getUrl() {
		return url;
	}

	public void setUrl(@NonNull String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@NonNull
	@Override
	public String toString() {
		return description;
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Link other = (Link) obj;
		return url.equals(other.url);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(id);
		out.writeLong(eventId);
		out.writeString(url);
		out.writeString(description);
	}

	public static final Parcelable.Creator<Link> CREATOR = new Parcelable.Creator<Link>() {
		public Link createFromParcel(Parcel in) {
			return new Link(in);
		}

		public Link[] newArray(int size) {
			return new Link[size];
		}
	};

	Link(Parcel in) {
		id = in.readLong();
		eventId = in.readLong();
		url = in.readString();
		description = in.readString();
	}
}
