package be.digitalia.fosdem.model;

import be.digitalia.fosdem.api.FosdemUrls;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.utils.StringUtils;
import android.os.Parcel;
import android.os.Parcelable;

public class Person implements Parcelable {

	private long id;
	private String name;

	public Person() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return FosdemUrls.getPerson(StringUtils.toSlug(name), DatabaseManager.getInstance().getYear());
	}

	@Override
	public String toString() {
		return name;
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
		if (getClass() != obj.getClass())
			return false;
		Person other = (Person) obj;
		return (id == other.id);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(id);
		out.writeString(name);
	}

	public static final Parcelable.Creator<Person> CREATOR = new Parcelable.Creator<Person>() {
		public Person createFromParcel(Parcel in) {
			return new Person(in);
		}

		public Person[] newArray(int size) {
			return new Person[size];
		}
	};

	private Person(Parcel in) {
		id = in.readLong();
		name = in.readString();
	}
}
