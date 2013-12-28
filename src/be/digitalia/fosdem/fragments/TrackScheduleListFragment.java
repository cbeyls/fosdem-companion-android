package be.digitalia.fosdem.fragments;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.DateUtils;

public class TrackScheduleListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private static final int EVENTS_LOADER_ID = 1;
	private static final String ARG_DAY = "day";
	private static final String ARG_TRACK = "track";

	private TrackScheduleAdapter adapter;

	public static TrackScheduleListFragment newInstance(Day day, Track track) {
		TrackScheduleListFragment f = new TrackScheduleListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_DAY, day);
		args.putParcelable(ARG_TRACK, track);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new TrackScheduleAdapter(getActivity());
		setListAdapter(adapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.no_data));
		setListShown(false);

		getLoaderManager().initLoader(EVENTS_LOADER_ID, null, this);
	}

	private static class TrackScheduleLoader extends SimpleCursorLoader {

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

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Day day = getArguments().getParcelable(ARG_DAY);
		Track track = getArguments().getParcelable(ARG_TRACK);
		return new TrackScheduleLoader(getActivity(), day, track);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			adapter.swapCursor(data);
		}

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO
	}

	private static class TrackScheduleAdapter extends CursorAdapter {

		private static final DateFormat START_TIME_DATE_FORMAT = DateUtils.withBelgiumTimeZone(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT,
				Locale.getDefault()));

		private final LayoutInflater inflater;
		private final int titleTextSize;

		public TrackScheduleAdapter(Context context) {
			super(context, null, 0);
			inflater = LayoutInflater.from(context);
			titleTextSize = context.getResources().getDimensionPixelSize(R.dimen.list_title_text_size);
		}

		@Override
		public Event getItem(int position) {
			return DatabaseManager.toEvent((Cursor) super.getItem(position));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = inflater.inflate(R.layout.item_schedule_event, parent, false);

			ViewHolder holder = new ViewHolder();
			holder.time = (TextView) view.findViewById(R.id.time);
			holder.text = (TextView) view.findViewById(R.id.text);
			holder.titleSizeSpan = new AbsoluteSizeSpan(titleTextSize);
			holder.boldStyleSpan = new StyleSpan(Typeface.BOLD);
			view.setTag(holder);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			Event event = DatabaseManager.toEvent(cursor, holder.event);
			holder.event = event;
			holder.time.setText(START_TIME_DATE_FORMAT.format(event.getStartTime()));

			String eventTitle = event.getTitle();
			String personsSummary = event.getPersonsSummary();
			SpannableString spannableString = new SpannableString(String.format("%1$s\n%2$s\n%3$s", eventTitle, personsSummary, event.getRoomName()));
			spannableString.setSpan(holder.titleSizeSpan, 0, eventTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			spannableString.setSpan(holder.boldStyleSpan, 0, eventTitle.length() + personsSummary.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			holder.text.setText(spannableString);
		}

		private static class ViewHolder {
			TextView time;
			TextView text;
			AbsoluteSizeSpan titleSizeSpan;
			StyleSpan boldStyleSpan;
			Event event;
		}
	}
}
