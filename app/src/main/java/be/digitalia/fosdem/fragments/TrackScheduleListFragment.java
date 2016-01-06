package be.digitalia.fosdem.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.TextViewCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.TrackScheduleLoader;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.DateUtils;

public class TrackScheduleListFragment extends SmoothListFragment implements Handler.Callback, LoaderCallbacks<Cursor> {

	/**
	 * Interface implemented by container activities
	 */
	public interface Callbacks {
		void onEventSelected(int position, Event event);
	}

	private static final int EVENTS_LOADER_ID = 1;
	private static final int REFRESH_TIME_WHAT = 1;
	private static final long REFRESH_TIME_INTERVAL = 60 * 1000L; // 1min

	private static final String ARG_DAY = "day";
	private static final String ARG_TRACK = "track";
	private static final String ARG_FROM_EVENT_ID = "from_event_id";

	private Day day;
	private Handler handler;
	private TrackScheduleAdapter adapter;
	private Callbacks listener;
	private boolean selectionEnabled = false;
	private boolean isListAlreadyShown = false;

	public static TrackScheduleListFragment newInstance(Day day, Track track) {
		TrackScheduleListFragment f = new TrackScheduleListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_DAY, day);
		args.putParcelable(ARG_TRACK, track);
		f.setArguments(args);
		return f;
	}

	public static TrackScheduleListFragment newInstance(Day day, Track track, long fromEventId) {
		TrackScheduleListFragment f = new TrackScheduleListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_DAY, day);
		args.putParcelable(ARG_TRACK, track);
		args.putLong(ARG_FROM_EVENT_ID, fromEventId);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		day = getArguments().getParcelable(ARG_DAY);
		handler = new Handler(this);
		adapter = new TrackScheduleAdapter(getActivity());
		setListAdapter(adapter);

		if (savedInstanceState != null) {
			isListAlreadyShown = savedInstanceState.getBoolean("isListAlreadyShown");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("isListAlreadyShown", isListAlreadyShown);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof Callbacks) {
			listener = (Callbacks) activity;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}

	private void notifyEventSelected(int position) {
		if (listener != null) {
			listener.onEventSelected(position, (position == ListView.INVALID_POSITION) ? null : adapter.getItem(position));
		}
	}

	public void setSelectionEnabled(boolean selectionEnabled) {
		this.selectionEnabled = selectionEnabled;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setChoiceMode(selectionEnabled ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		setEmptyText(getString(R.string.no_data));
		setListShown(false);

		getLoaderManager().initLoader(EVENTS_LOADER_ID, null, this);
	}

	@Override
	public void onStart() {
		super.onStart();

		// Setup display auto-refresh during the track's day
		long now = System.currentTimeMillis();
		long dayStart = day.getDate().getTime();
		if (now < dayStart) {
			// Before track day, schedule refresh in the future
			handler.sendEmptyMessageDelayed(REFRESH_TIME_WHAT, dayStart - now);
		} else if (now < dayStart + android.text.format.DateUtils.DAY_IN_MILLIS) {
			// During track day, start refresh immediately
			adapter.setCurrentTime(now);
			handler.sendEmptyMessageDelayed(REFRESH_TIME_WHAT, REFRESH_TIME_INTERVAL);
		} else {
			// After track day, disable refresh
			adapter.setCurrentTime(-1L);
		}
	}

	@Override
	public void onStop() {
		handler.removeMessages(REFRESH_TIME_WHAT);
		super.onStop();
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case REFRESH_TIME_WHAT:
				adapter.setCurrentTime(System.currentTimeMillis());
				handler.sendEmptyMessageDelayed(REFRESH_TIME_WHAT, REFRESH_TIME_INTERVAL);
				return true;
		}
		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Track track = getArguments().getParcelable(ARG_TRACK);
		return new TrackScheduleLoader(getActivity(), day, track);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			adapter.swapCursor(data);

			if (selectionEnabled) {
				final int count = adapter.getCount();
				int checkedPosition = getListView().getCheckedItemPosition();
				if ((checkedPosition == ListView.INVALID_POSITION) || (checkedPosition >= count)) {
					// There is no current valid selection, use the default one
					checkedPosition = getDefaultPosition();
					if (checkedPosition != ListView.INVALID_POSITION) {
						getListView().setItemChecked(checkedPosition, true);
					}
				}

				// Ensure the current selection is visible
				if (checkedPosition != ListView.INVALID_POSITION) {
					setSelection(checkedPosition);
				}
				// Notify the parent of the current selection to synchronize its state
				notifyEventSelected(checkedPosition);

			} else if (!isListAlreadyShown) {
				int position = getDefaultPosition();
				if (position != ListView.INVALID_POSITION) {
					setSelection(position);
				}
			}
			isListAlreadyShown = true;
		}

		setListShown(true);
	}

	/**
	 * @return The default position in the list, or -1 if the list is empty
	 */
	private int getDefaultPosition() {
		final int count = adapter.getCount();
		if (count == 0) {
			return ListView.INVALID_POSITION;
		}
		long fromEventId = getArguments().getLong(ARG_FROM_EVENT_ID, -1L);
		if (fromEventId != -1L) {
			// Look for the source event in the list and return its position
			for (int i = 0; i < count; ++i) {
				if (adapter.getItemId(i) == fromEventId) {
					return i;
				}
			}
		}
		return 0;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		notifyEventSelected(position);
	}

	private static class TrackScheduleAdapter extends CursorAdapter {

		private static final int[] PRIMARY_TEXT_COLORS
				= new int[]{android.R.attr.textColorPrimary, android.R.attr.textColorPrimaryInverse};

		private final LayoutInflater inflater;
		private final DateFormat timeDateFormat;
		private final int timeBackgroundColor;
		private final int timeForegroundColor;
		private final int timeRunningBackgroundColor;
		private final int timeRunningForegroundColor;
		private long currentTime = -1L;

		public TrackScheduleAdapter(Context context) {
			super(context, null, 0);
			inflater = LayoutInflater.from(context);
			timeDateFormat = DateUtils.getTimeDateFormat(context);
			timeBackgroundColor = ContextCompat.getColor(context, R.color.schedule_time_background);
			timeRunningBackgroundColor = ContextCompat.getColor(context, R.color.schedule_time_running_background);

			TypedArray a = context.getTheme().obtainStyledAttributes(PRIMARY_TEXT_COLORS);
			timeForegroundColor = a.getColor(0, 0);
			timeRunningForegroundColor = a.getColor(1, 0);
			a.recycle();
		}

		public void setCurrentTime(long time) {
			if (currentTime != time) {
				currentTime = time;
				notifyDataSetChanged();
			}
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
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.persons = (TextView) view.findViewById(R.id.persons);
			holder.room = (TextView) view.findViewById(R.id.room);
			view.setTag(holder);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			Event event = DatabaseManager.toEvent(cursor, holder.event);
			holder.event = event;

			holder.time.setText(timeDateFormat.format(event.getStartTime()));
			if ((currentTime != -1L) && event.isRunningAtTime(currentTime)) {
				// Contrast colors for running event
				holder.time.setBackgroundColor(timeRunningBackgroundColor);
				holder.time.setTextColor(timeRunningForegroundColor);
			} else {
				// Normal colors
				holder.time.setBackgroundColor(timeBackgroundColor);
				holder.time.setTextColor(timeForegroundColor);
			}

			holder.title.setText(event.getTitle());
			int bookmarkDrawable = DatabaseManager.toBookmarkStatus(cursor) ? R.drawable.ic_bookmark_grey600_24dp : 0;
			TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(holder.title, 0, 0, bookmarkDrawable, 0);
			String personsSummary = event.getPersonsSummary();
			holder.persons.setText(personsSummary);
			holder.persons.setVisibility(TextUtils.isEmpty(personsSummary) ? View.GONE : View.VISIBLE);
			holder.room.setText(event.getRoomName());
		}

		private static class ViewHolder {
			TextView time;
			TextView title;
			TextView persons;
			TextView room;
			Event event;
		}
	}
}
