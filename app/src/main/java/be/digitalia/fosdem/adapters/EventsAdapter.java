package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.TextViewCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.utils.DateUtils;

public class EventsAdapter extends CursorAdapter {

	protected final LayoutInflater inflater;
	protected final DateFormat timeDateFormat;
	private final boolean showDay;

	public EventsAdapter(Context context) {
		this(context, true);
	}

	public EventsAdapter(Context context, boolean showDay) {
		super(context, null, 0);
		inflater = LayoutInflater.from(context);
		timeDateFormat = DateUtils.getTimeDateFormat(context);
		this.showDay = showDay;
	}

	@Override
	public Event getItem(int position) {
		return DatabaseManager.toEvent((Cursor) super.getItem(position));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = inflater.inflate(R.layout.item_event, parent, false);

		ViewHolder holder = new ViewHolder();
		holder.title = (TextView) view.findViewById(R.id.title);
		holder.persons = (TextView) view.findViewById(R.id.persons);
		holder.trackName = (TextView) view.findViewById(R.id.track_name);
		holder.details = (TextView) view.findViewById(R.id.details);
		view.setTag(holder);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		Event event = DatabaseManager.toEvent(cursor, holder.event);
		holder.event = event;

		holder.title.setText(event.getTitle());
		int bookmarkDrawable = DatabaseManager.toBookmarkStatus(cursor) ? R.drawable.ic_bookmark_grey600_24dp : 0;
		TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(holder.title, 0, 0, bookmarkDrawable, 0);
		String personsSummary = event.getPersonsSummary();
		holder.persons.setText(personsSummary);
		holder.persons.setVisibility(TextUtils.isEmpty(personsSummary) ? View.GONE : View.VISIBLE);
		holder.trackName.setText(event.getTrack().getName());

		Date startTime = event.getStartTime();
		Date endTime = event.getEndTime();
		String startTimeString = (startTime != null) ? timeDateFormat.format(startTime) : "?";
		String endTimeString = (endTime != null) ? timeDateFormat.format(endTime) : "?";
		String details;
		if (showDay) {
			details = String.format("%1$s, %2$s ― %3$s  |  %4$s", event.getDay().getShortName(), startTimeString, endTimeString, event.getRoomName());
		} else {
			details = String.format("%1$s ― %2$s  |  %3$s", startTimeString, endTimeString, event.getRoomName());
		}
		holder.details.setText(details);
	}

	protected static class ViewHolder {
		TextView title;
		TextView persons;
		TextView trackName;
		TextView details;
		Event event;
	}
}
