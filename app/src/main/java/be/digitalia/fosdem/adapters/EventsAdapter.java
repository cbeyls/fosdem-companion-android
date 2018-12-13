package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.DateUtils;
import be.digitalia.fosdem.widgets.MultiChoiceHelper;

public class EventsAdapter extends RecyclerViewCursorAdapter<EventsAdapter.ViewHolder>
		implements Observer<Map<String, RoomStatus>> {

	private static final Object DETAILS_PAYLOAD = new Object();

	protected final LayoutInflater inflater;
	protected final DateFormat timeDateFormat;
	private final boolean showDay;
	protected Map<String, RoomStatus> roomStatuses;

	public EventsAdapter(Context context, LifecycleOwner owner) {
		this(context, owner, true);
	}

	public EventsAdapter(Context context, LifecycleOwner owner, boolean showDay) {
		inflater = LayoutInflater.from(context);
		timeDateFormat = DateUtils.getTimeDateFormat(context);
		this.showDay = showDay;

		FosdemApi.getRoomStatuses().observe(owner, this);
	}

	@Override
	public void onChanged(@Nullable Map<String, RoomStatus> roomStatuses) {
		this.roomStatuses = roomStatuses;
		notifyItemRangeChanged(0, getItemCount(), DETAILS_PAYLOAD);
	}

	@Override
	public Event getItem(int position) {
		return DatabaseManager.toEvent((Cursor) super.getItem(position));
	}

	@Override
	public int getItemViewType(int position) {
		return R.layout.item_event;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.item_event, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
		Context context = holder.itemView.getContext();
		Event event = DatabaseManager.toEvent(cursor, holder.event);
		holder.event = event;

		holder.title.setText(event.getTitle());
		boolean isBookmarked = DatabaseManager.toBookmarkStatus(cursor);
		Drawable bookmarkDrawable = isBookmarked
				? AppCompatResources.getDrawable(context, R.drawable.ic_bookmark_grey600_24dp)
				: null;
		TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(holder.title, null, null, bookmarkDrawable, null);
		holder.title.setContentDescription(isBookmarked
				? context.getString(R.string.in_bookmarks_content_description, event.getTitle())
				: null
		);
		String personsSummary = event.getPersonsSummary();
		holder.persons.setText(personsSummary);
		holder.persons.setVisibility(TextUtils.isEmpty(personsSummary) ? View.GONE : View.VISIBLE);
		Track track = event.getTrack();
		holder.trackName.setText(track.getName());
		holder.trackName.setTextColor(ContextCompat.getColor(context, track.getType().getColorResId()));
		holder.trackName.setContentDescription(context.getString(R.string.track_content_description, track.getName()));

		bindDetails(holder, event);
	}

	protected void bindDetails(ViewHolder holder, Event event) {
		Context context = holder.details.getContext();
		Date startTime = event.getStartTime();
		Date endTime = event.getEndTime();
		String startTimeString = (startTime != null) ? timeDateFormat.format(startTime) : "?";
		String endTimeString = (endTime != null) ? timeDateFormat.format(endTime) : "?";
		String roomName = event.getRoomName();
		CharSequence details;
		if (showDay) {
			details = String.format("%1$s, %2$s ― %3$s  |  %4$s", event.getDay().getShortName(), startTimeString, endTimeString, roomName);
		} else {
			details = String.format("%1$s ― %2$s  |  %3$s", startTimeString, endTimeString, roomName);
		}
		CharSequence detailsDescription = details;
		if (roomStatuses != null) {
			RoomStatus roomStatus = roomStatuses.get(roomName);
			if (roomStatus != null) {
				SpannableString detailsSpannable = new SpannableString(details);
				int color = ContextCompat.getColor(context, roomStatus.getColorResId());
				detailsSpannable.setSpan(new ForegroundColorSpan(color),
						details.length() - roomName.length(),
						details.length(),
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				details = detailsSpannable;

				detailsDescription = String.format("%1$s (%2$s)", detailsDescription, context.getString(roomStatus.getNameResId()));
			}
		}
		holder.details.setText(details);
		holder.details.setContentDescription(context.getString(R.string.details_content_description, detailsDescription));
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (payloads.isEmpty()) {
			onBindViewHolder(holder, position);
		} else {
			if (payloads.contains(DETAILS_PAYLOAD) && (holder.event != null)) {
				bindDetails(holder, holder.event);
			}
		}
	}

	static class ViewHolder extends MultiChoiceHelper.ViewHolder implements View.OnClickListener {
		TextView title;
		TextView persons;
		TextView trackName;
		TextView details;

		Event event;
		boolean isOverlapping;

		public ViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			persons = itemView.findViewById(R.id.persons);
			trackName = itemView.findViewById(R.id.track_name);
			details = itemView.findViewById(R.id.details);
			setOnClickListener(this);
		}

		@Override
		public void onClick(View view) {
			Context context = view.getContext();
			Intent intent = new Intent(context, EventDetailsActivity.class)
					.putExtra(EventDetailsActivity.EXTRA_EVENT, event);
			context.startActivity(intent);
		}
	}
}
