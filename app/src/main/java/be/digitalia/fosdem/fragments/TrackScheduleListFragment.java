package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.os.Bundle;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.TrackScheduleAdapter;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.viewmodels.TrackScheduleViewModel;

public class TrackScheduleListFragment extends RecyclerViewFragment
		implements TrackScheduleAdapter.EventClickListener, Observer<List<StatusEvent>> {

	/**
	 * Interface implemented by container activities
	 */
	public interface Callbacks {
		void onEventSelected(int position, Event event);
	}

	private static final String ARG_DAY = "day";
	private static final String ARG_TRACK = "track";
	private static final String ARG_FROM_EVENT_ID = "from_event_id";

	private static final String STATE_IS_LIST_ALREADY_SHOWN = "isListAlreadyShown";
	private static final String STATE_SELECTED_ID = "selectedId";

	private TrackScheduleAdapter adapter;
	private TrackScheduleViewModel viewModel;
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

		adapter = new TrackScheduleAdapter(getActivity(), this);

		final Bundle args = requireArguments();
		final Day day = args.getParcelable(ARG_DAY);
		final Track track = args.getParcelable(ARG_TRACK);
		viewModel = new ViewModelProvider(this).get(TrackScheduleViewModel.class);
		viewModel.setTrack(day, track);
		viewModel.getCurrentTime().observe(this, now -> adapter.setCurrentTime(now));

		if (savedInstanceState != null) {
			isListAlreadyShown = savedInstanceState.getBoolean(STATE_IS_LIST_ALREADY_SHOWN);
		}
		if (savedInstanceState == null) {
			setSelectedId(args.getLong(ARG_FROM_EVENT_ID, -1L));
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
	public void onAttach(@NonNull Context context) {
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

		viewModel.getSchedule().observe(getViewLifecycleOwner(), this);
	}

	@Override
	public void onEventClick(int position, Event event) {
		setSelectedId(event.getId());
		notifyEventSelected(position, event);
	}

	@Override
	public void onChanged(List<StatusEvent> schedule) {
		adapter.submitList(schedule);

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
			notifyEventSelected(selectedPosition, (selectedPosition == RecyclerView.NO_POSITION) ? null : schedule.get(selectedPosition).getEvent());

		} else if (!isListAlreadyShown) {
			final int position = adapter.getPositionForId(selectedId);
			if (position != RecyclerView.NO_POSITION) {
				getRecyclerView().scrollToPosition(position);
			}
		}
		isListAlreadyShown = true;

		setProgressBarVisible(false);
	}
}
