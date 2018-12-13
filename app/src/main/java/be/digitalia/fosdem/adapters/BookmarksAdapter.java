package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.Date;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.widgets.MultiChoiceHelper;

public class BookmarksAdapter extends EventsAdapter {

	@ColorInt
	private final int errorColor;
	final MultiChoiceHelper multiChoiceHelper;

	public BookmarksAdapter(AppCompatActivity activity, LifecycleOwner owner) {
		super(activity, owner);
		errorColor = ContextCompat.getColor(activity, R.color.error_material);
		multiChoiceHelper = new MultiChoiceHelper(activity, this);
		multiChoiceHelper.setMultiChoiceModeListener(new MultiChoiceHelper.MultiChoiceModeListener() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater().inflate(R.menu.action_mode_bookmarks, menu);
				return true;
			}

			private void updateSelectedCountDisplay(ActionMode mode) {
				int count = multiChoiceHelper.getCheckedItemCount();
				mode.setTitle(multiChoiceHelper.getContext().getResources().getQuantityString(R.plurals.selected, count, count));
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				updateSelectedCountDisplay(mode);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
					case R.id.delete:
						// Remove multiple bookmarks at once
						new RemoveBookmarksAsyncTask().execute(multiChoiceHelper.getCheckedItemIds());
						mode.finish();
						return true;
				}
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				updateSelectedCountDisplay(mode);
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}
		});
	}

	public Parcelable onSaveInstanceState() {
		return multiChoiceHelper.onSaveInstanceState();
	}

	public void onRestoreInstanceState(Parcelable state) {
		multiChoiceHelper.onRestoreInstanceState(state);
	}

	public void onDestroyView() {
		multiChoiceHelper.clearChoices();
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
		final int position = cursor.getPosition();
		Context context = holder.itemView.getContext();
		Event event = DatabaseManager.toEvent(cursor, holder.event);
		holder.event = event;
		holder.isOverlapping = isOverlapping(cursor, event.getStartTime(), event.getEndTime());

		holder.title.setText(event.getTitle());
		String personsSummary = event.getPersonsSummary();
		holder.persons.setText(personsSummary);
		holder.persons.setVisibility(TextUtils.isEmpty(personsSummary) ? View.GONE : View.VISIBLE);
		Track track = event.getTrack();
		holder.trackName.setText(track.getName());
		holder.trackName.setTextColor(ContextCompat.getColor(context, track.getType().getColorResId()));
		holder.trackName.setContentDescription(context.getString(R.string.track_content_description, track.getName()));

		bindDetails(holder, event);

		// Enable MultiChoice selection and update checked state
		holder.bind(multiChoiceHelper, position);
	}

	@Override
	protected void bindDetails(ViewHolder holder, Event event) {
		Context context = holder.details.getContext();
		Date startTime = event.getStartTime();
		Date endTime = event.getEndTime();
		String startTimeString = (startTime != null) ? timeDateFormat.format(startTime) : "?";
		String endTimeString = (endTime != null) ? timeDateFormat.format(endTime) : "?";
		String roomName = event.getRoomName();
		String details = String.format("%1$s, %2$s ― %3$s  |  %4$s", event.getDay().getShortName(), startTimeString, endTimeString, roomName);
		SpannableString detailsSpannable = new SpannableString(details);
		String detailsContentDescription = details;

		// Highlight the date and time with error color in case of conflicting schedules
		if (holder.isOverlapping) {
			int endPosition = details.indexOf(" | ");
			detailsSpannable.setSpan(new ForegroundColorSpan(errorColor), 0, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			detailsSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			holder.details.setText(detailsSpannable);
			detailsContentDescription = context.getString(R.string.bookmark_conflict_content_description, detailsContentDescription);
		}
		if (roomStatuses != null) {
			RoomStatus roomStatus = roomStatuses.get(roomName);
			if (roomStatus != null) {
				int color = ContextCompat.getColor(context, roomStatus.getColorResId());
				detailsSpannable.setSpan(new ForegroundColorSpan(color),
						details.length() - roomName.length(),
						details.length(),
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		holder.details.setText(detailsSpannable);
		holder.details.setContentDescription(context.getString(R.string.details_content_description, detailsContentDescription));
	}

	/**
	 * Checks if the current event is overlapping with the previous or next one.
	 * Warning: this methods will update the cursor's position.
	 */
	private static boolean isOverlapping(Cursor cursor, Date startTime, Date endTime) {
		final int position = cursor.getPosition();

		if ((startTime != null) && (position > 0) && cursor.moveToPosition(position - 1)) {
			long previousEndTime = DatabaseManager.toEventEndTimeMillis(cursor);
			if ((previousEndTime != -1L) && (previousEndTime > startTime.getTime())) {
				// The event overlaps with the previous one
				return true;
			}
		}

		if ((endTime != null) && (position < (cursor.getCount() - 1)) && cursor.moveToPosition(position + 1)) {
			long nextStartTime = DatabaseManager.toEventStartTimeMillis(cursor);
			if ((nextStartTime != -1L) && (nextStartTime < endTime.getTime())) {
				// The event overlaps with the next one
				return true;
			}
		}

		return false;
	}

	static class RemoveBookmarksAsyncTask extends AsyncTask<long[], Void, Void> {

		@Override
		protected Void doInBackground(long[]... params) {
			DatabaseManager.getInstance().removeBookmarks(params[0]);
			return null;
		}

	}
}
