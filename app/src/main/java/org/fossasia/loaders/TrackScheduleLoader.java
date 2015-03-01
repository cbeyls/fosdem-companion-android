package org.fossasia.loaders;

import android.content.Context;
import android.database.Cursor;
import org.fossasia.db.DatabaseManager;
import org.fossasia.model.Day;
import org.fossasia.model.Track;

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
