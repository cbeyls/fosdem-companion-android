package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.TrackScheduleAdapter;
import be.digitalia.fosdem.loaders.TrackScheduleLoader;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Track;

public class TrackScheduleListFragment extends RecyclerViewFragment
		implements TrackScheduleAdapter.EventClickListener, Handler.Callback, LoaderCallbacks<Cursor> {

	/**
	 * Interface implemented by container activities
	 */
	public interface Callbacks {
		void onEventSelected(int position, Event event);
	}

	private static final int EVENTS_LOADER_ID = 1;
	private static final int REFRESH_TIME_WHAT = 1;
	private static final long REFRESH_TIME_INTERVAL = DateUtils.MINUTE_IN_MILLIS;

	private static final String ARG_DAY = "day";
	private static final String ARG_TRACK = "track";
	private static final String ARG_FROM_EVENT_ID = "from_event_id";

	private static final String STATE_IS_LIST_ALREADY_SHOWN = "isListAlreadyShown";
	private static final String STATE_SELECTED_ID = "selectedId";

	private Day day;
	private Handler handler;
	private TrackScheduleAdapter adapter;
	private Callbacks listener;
	private boolean selectionEnabled = false;
	private long selectedId = -1L;
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
		selectionEnabled = getResources().getBoolean(R.bool.tablet_landscape);

		day = getArguments().getParcelable(ARG_DAY);
		handler = new Handler(this);
		adapter = new TrackScheduleAdapter(getActivity(), this);

		if (savedInstanceState != null) {
			isListAlreadyShown = savedInstanceState.getBoolean(STATE_IS_LIST_ALREADY_SHOWN);
		}
		if (savedInstanceState == null) {
			setSelectedId(getArguments().getLong(ARG_FROM_EVENT_ID, -1L));
		} else {
			setSelectedId(savedInstanceState.getLong(STATE_SELECTED_ID));
		}
	}

	private void setSelectedId(long id) {
		selectedId = id;
		if (selectionEnabled) {
			adapter.setSelectedId(id);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_IS_LIST_ALREADY_SHOWN, isListAlreadyShown);
		outState.putLong(STATE_SELECTED_ID, selectedId);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof Callbacks) {
			listener = (Callbacks) context;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}

	private void notifyEventSelected(int position, Event event) {
		if (listener != null) {
			listener.onEventSelected(position, event);
		}
	}

	@Override
	protected void onRecyclerViewCreated(RecyclerView recyclerView, @Nullable Bundle savedInstanceState) {
		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
		recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setAdapter(adapter);
		setEmptyText(getString(R.string.no_data));
		setProgressBarVisible(true);

		LoaderManager.getInstance(this).initLoader(EVENTS_LOADER_ID, null, this);
	}

	@Override
	public void onStart() {
		super.onStart();

		// Setup display auto-refresh during the track's day
		long now = System.currentTimeMillis();
		long dayStart = day.getDate().getTime();
		if (now < dayStart) {
			// Before track day, schedule refresh in the future
			adapter.setCurrentTime(-1L);
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
	public void onEventClick(int position, Event event) {
		setSelectedId(event.getId());
		notifyEventSelected(position, event);
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

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Track track = getArguments().getParcelable(ARG_TRACK);
		return new TrackScheduleLoader(getActivity(), day, track);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			adapter.swapCursor(data);

			if (selectionEnabled) {
				int selectedPosition = adapter.getPositionForId(selectedId);
				if (selectedPosition == RecyclerView.NO_POSITION && adapter.getItemCount() > 0) {
					// There is no current valid selection, reset to use the first item
					setSelectedId(adapter.getItemId(0));
					selectedPosition = 0;
				}

				// Ensure the current selection is visible
				if (selectedPosition != RecyclerView.NO_POSITION) {
					getRecyclerView().scrollToPosition(selectedPosition);
				}
				// Notify the parent of the current selection to synchronize its state
				notifyEventSelected(selectedPosition, (selectedPosition == RecyclerView.NO_POSITION) ? null : adapter.getItem(selectedPosition));

			} else if (!isListAlreadyShown) {
				final int position = adapter.getPositionForId(selectedId);
				if (position != RecyclerView.NO_POSITION) {
					getRecyclerView().scrollToPosition(position);
				}
			}
			isListAlreadyShown = true;
		}

		setProgressBarVisible(false);
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
}
