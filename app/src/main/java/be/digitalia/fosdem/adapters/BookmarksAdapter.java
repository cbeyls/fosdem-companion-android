package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;

import java.util.Date;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Event;

public class BookmarksAdapter extends EventsAdapter {

	@ColorInt
	private final int errorColor;

	public BookmarksAdapter(Context context) {
		super(context);
		errorColor = ContextCompat.getColor(context, R.color.error_material);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		Event event = DatabaseManager.toEvent(cursor, holder.event);
		holder.event = event;

		holder.title.setText(event.getTitle());
		String personsSummary = event.getPersonsSummary();
		holder.persons.setText(personsSummary);
		holder.persons.setVisibility(TextUtils.isEmpty(personsSummary) ? View.GONE : View.VISIBLE);
		holder.trackName.setText(event.getTrack().getName());

		Date startTime = event.getStartTime();
		Date endTime = event.getEndTime();
		String startTimeString = (startTime != null) ? timeDateFormat.format(startTime) : "?";
		String endTimeString = (endTime != null) ? timeDateFormat.format(endTime) : "?";
		String details = String.format("%1$s, %2$s ― %3$s  |  %4$s", event.getDay().getShortName(), startTimeString, endTimeString, event.getRoomName());

		// Highlight the date and time with error color in case of conflicting schedules
		if (isOverlapping(cursor, startTime, endTime)) {
			SpannableString detailsSpannable = new SpannableString(details);
			int endPosition = details.indexOf(" | ");
			detailsSpannable.setSpan(new ForegroundColorSpan(errorColor), 0, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			detailsSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			holder.details.setText(detailsSpannable);
		} else {
			holder.details.setText(details);
		}
	}

	/**
	 * Checks if the current event is overlapping with the previous or next one.
	 */
	public static boolean isOverlapping(Cursor cursor, Date startTime, Date endTime) {
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
}
