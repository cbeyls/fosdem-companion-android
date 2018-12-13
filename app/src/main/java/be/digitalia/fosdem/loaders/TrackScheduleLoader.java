package be.digitalia.fosdem.loaders;

import android.content.Context;
import android.database.Cursor;

import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Track;

public class TrackScheduleLoader extends SimpleCursorLoader {

	private final Day day;
	private final Track track;

	public TrackScheduleLoader(Context context, Day day, Track track) {
		super(context);
		this.day = day;
		this.track = track;
	}

	@Override
	protected Cursor getCursor() {
		return DatabaseManager.getInstance().getEvents(day, track);
	}
}
