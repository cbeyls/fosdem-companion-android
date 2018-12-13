package be.digitalia.fosdem.model;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import be.digitalia.fosdem.R;

public enum RoomStatus {
	OPEN(R.string.room_status_open, R.color.room_status_open),
	FULL(R.string.room_status_full, R.color.room_status_full),
	EMERGENCY_EVACUATION(R.string.room_status_emergency_evacuation, R.color.room_status_emergency_evacuation);

	private final int nameResId;
	private final int colorResId;

	RoomStatus(@StringRes int nameResId, @ColorRes int colorResId) {
		this.nameResId = nameResId;
		this.colorResId = colorResId;
	}

	@StringRes
	public int getNameResId() {
		return nameResId;
	}

	@ColorRes
	public int getColorResId() {
		return colorResId;
	}
}
