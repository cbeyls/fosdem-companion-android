package be.digitalia.fosdem.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import be.digitalia.fosdem.R;

@Entity(tableName = Track.TABLE_NAME, indices = {@Index(value = {"name", "type"}, name = "track_main_idx", unique = true)})
public class Track implements Parcelable {

	public static final String TABLE_NAME = "tracks";

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

	@PrimaryKey
	private long id;
	@NonNull
	private String name;
	@NonNull
	private Type type;

	@Ignore
	public Track() {
	}

	public Track(@NonNull String name, @NonNull Type type) {
		this.name = name;
		this.type = type;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@NonNull
	public String getName() {
		return name;
	}

	public void setName(@NonNull String name) {
		this.name = name;
	}

	@NonNull
	public Type getType() {
		return type;
	}

	public void setType(@NonNull Type type) {
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
		out.writeLong(id);
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
		id = in.readLong();
		name = in.readString();
		type = Type.values()[in.readInt()];
	}
}
