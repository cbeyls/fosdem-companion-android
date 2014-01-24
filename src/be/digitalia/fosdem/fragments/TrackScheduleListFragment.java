package be.digitalia.fosdem.fragments;

import java.text.DateFormat;

import android.app.Activity;
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
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.TrackScheduleLoader;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.DateUtils;

public class TrackScheduleListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	/**
	 * Interface implemented by container activities
	 */
	public interface Callbacks {
		void onEventSelected(int position, Event event);
	}

	private static final int EVENTS_LOADER_ID = 1;
	private static final String ARG_DAY = "day";
	private static final String ARG_TRACK = "track";

	private TrackScheduleAdapter adapter;
	private Callbacks listener;
	private boolean selectionEnabled = false;

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
			listener.onEventSelected(position, (position == -1) ? null : adapter.getItem(position));
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
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Day day = getArguments().getParcelable(ARG_DAY);
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
					if (count > 0) {
						// Select the first item if any
						getListView().setItemChecked(0, true);
						checkedPosition = 0;
					} else {
						// No result, nothing selected
						checkedPosition = -1;
					}
				}

				// Ensure the current selection is visible
				if (checkedPosition != -1) {
					setSelection(checkedPosition);
				}
				// Notify the parent of the current selection to synchronize its state
				notifyEventSelected(checkedPosition);
			}
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
		notifyEventSelected(position);
	}

	private static class TrackScheduleAdapter extends CursorAdapter {

		private static final DateFormat TIME_DATE_FORMAT = DateUtils.getTimeDateFormat();

		private final LayoutInflater inflater;
		private final int titleTextSize;

		public TrackScheduleAdapter(Context context) {
			super(context, null, 0);
			inflater = LayoutInflater.from(context);
			titleTextSize = context.getResources().getDimensionPixelSize(R.dimen.list_item_title_text_size);
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
			holder.time.setText(TIME_DATE_FORMAT.format(event.getStartTime()));

			SpannableString spannableString;
			String eventTitle = event.getTitle();
			String personsSummary = event.getPersonsSummary();
			if (TextUtils.isEmpty(personsSummary)) {
				spannableString = new SpannableString(String.format("%1$s\n%2$s", eventTitle, event.getRoomName()));
			} else {
				spannableString = new SpannableString(String.format("%1$s\n%2$s\n%3$s", eventTitle, personsSummary, event.getRoomName()));
			}
			spannableString.setSpan(holder.titleSizeSpan, 0, eventTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			spannableString.setSpan(holder.boldStyleSpan, 0, eventTitle.length() + personsSummary.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			holder.text.setText(spannableString);
			int bookmarkDrawable = DatabaseManager.toBookmarkStatus(cursor) ? R.drawable.ic_small_starred : 0;
			holder.text.setCompoundDrawablesWithIntrinsicBounds(0, 0, bookmarkDrawable, 0);
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
