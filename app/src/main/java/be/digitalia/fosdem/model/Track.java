package be.digitalia.fosdem.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import be.digitalia.fosdem.R;

public class Track implements Parcelable {

	public enum Type {
		other(R.string.other, R.color.track_type_other, R.color.track_type_other_dark),
		keynote(R.string.keynote, R.color.track_type_keynote, R.color.track_type_keynote_dark),
		maintrack(R.string.main_track, R.color.track_type_main, R.color.track_type_main_dark),
		devroom(R.string.developer_room, R.color.track_type_developer_room, R.color.track_type_developer_room_dark),
		lightningtalk(R.string.lightning_talk, R.color.track_type_lightning_talk, R.color.track_type_lightning_talk_dark),
		certification(R.string.certification_exam, R.color.track_type_certification_exam, R.color.track_type_certification_exam_dark);

		private final int nameResId;
		private final int colorResId;
		private final int darkColorResId;

		Type(@StringRes int nameResId, @ColorRes int colorResId, @ColorRes int darkColorResId) {
			this.nameResId = nameResId;
			this.colorResId = colorResId;
			this.darkColorResId = darkColorResId;
		}

		@StringRes
		public int getNameResId() {
			return nameResId;
		}

		@ColorRes
		public int getColorResId() {
			return colorResId;
		}

		@ColorRes
		public int getDarkColorResId() {
			return darkColorResId;
		}
	}

	private String name;
	private Type type;

	public Track() {
	}

	public Track(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + type.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Track other = (Track) obj;
		return name.equals(other.name) && (type == other.type);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeInt(type.ordinal());
	}

	public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
		public Track createFromParcel(Parcel in) {
			return new Track(in);
		}

		public Track[] newArray(int size) {
			return new Track[size];
		}
	};

	Track(Parcel in) {
		name = in.readString();
		type = Type.values()[in.readInt()];
	}
}
