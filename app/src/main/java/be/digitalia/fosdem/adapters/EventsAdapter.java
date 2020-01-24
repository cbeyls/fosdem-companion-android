package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.content.Intent;
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
import androidx.core.util.ObjectsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.DateUtils;

public class EventsAdapter extends PagedListAdapter<StatusEvent, EventsAdapter.ViewHolder>
		implements Observer<Map<String, RoomStatus>> {

	private static final DiffUtil.ItemCallback<StatusEvent> DIFF_CALLBACK = new SimpleItemCallback<StatusEvent>() {
		@Override
		public boolean areContentsTheSame(@NonNull StatusEvent oldItem, @NonNull StatusEvent newItem) {
			final Event oldEvent = oldItem.getEvent();
			final Event newEvent = newItem.getEvent();
			return ObjectsCompat.equals(oldEvent.getTitle(), newEvent.getTitle())
					&& ObjectsCompat.equals(oldEvent.getPersonsSummary(), newEvent.getPersonsSummary())
					&& ObjectsCompat.equals(oldEvent.getTrack(), newEvent.getTrack())
					&& ObjectsCompat.equals(oldEvent.getDay(), newEvent.getDay())
					&& ObjectsCompat.equals(oldEvent.getStartTime(), newEvent.getStartTime())
					&& ObjectsCompat.equals(oldEvent.getEndTime(), newEvent.getEndTime())
					&& ObjectsCompat.equals(oldEvent.getRoomName(), newEvent.getRoomName())
					&& oldItem.isBookmarked() == newItem.isBookmarked();
		}
	};
	private static final Object DETAILS_PAYLOAD = new Object();

	private final DateFormat timeDateFormat;
	private final boolean showDay;
	private Map<String, RoomStatus> roomStatuses;

	public EventsAdapter(Context context, LifecycleOwner owner) {
		this(context, owner, true);
	}

	public EventsAdapter(Context context, LifecycleOwner owner, boolean showDay) {
		super(DIFF_CALLBACK);
		timeDateFormat = DateUtils.getTimeDateFormat(context);
		this.showDay = showDay;

		FosdemApi.getRoomStatuses(context).observe(owner, this);
	}

	@Override
	public void onChanged(@Nullable Map<String, RoomStatus> roomStatuses) {
		this.roomStatuses = roomStatuses;
		notifyItemRangeChanged(0, getItemCount(), DETAILS_PAYLOAD);
	}

	@Override
	public int getItemViewType(int position) {
		return R.layout.item_event;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
		return new ViewHolder(view, timeDateFormat);
	}

	private RoomStatus getRoomStatus(Event event) {
		return (roomStatuses == null) ? null : roomStatuses.get(event.getRoomName());
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		final StatusEvent statusEvent = getItem(position);
		if (statusEvent == null) {
			holder.clear();
		} else {
			final Event event = statusEvent.getEvent();
			holder.bind(event, statusEvent.isBookmarked());
			holder.bindDetails(event, showDay, getRoomStatus(event));
		}
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (payloads.isEmpty()) {
			onBindViewHolder(holder, position);
		} else {
			final StatusEvent statusEvent = getItem(position);
			if (statusEvent != null) {
				if (payloads.contains(DETAILS_PAYLOAD)) {
					final Event event = statusEvent.getEvent();
					holder.bindDetails(event, showDay, getRoomStatus(event));
				}
			}
		}
	}

	static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		final TextView title;
		final TextView persons;
		final TextView trackName;
		final TextView details;

		private final DateFormat timeDateFormat;

		Event event;

		ViewHolder(View itemView, @NonNull DateFormat timeDateFormat) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			persons = itemView.findViewById(R.id.persons);
			trackName = itemView.findViewById(R.id.track_name);
			details = itemView.findViewById(R.id.details);
			itemView.setOnClickListener(this);

			this.timeDateFormat = timeDateFormat;
		}

		void clear() {
			this.event = null;
			title.setText(null);
			persons.setText(null);
			trackName.setText(null);
			details.setText(null);
		}

		void bind(@NonNull Event event, boolean isBookmarked) {
			Context context = itemView.getContext();
			this.event = event;

			title.setText(event.getTitle());
			Drawable bookmarkDrawable = isBookmarked
					? AppCompatResources.getDrawable(context, R.drawable.ic_bookmark_24dp)
					: null;
			TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(title, null, null, bookmarkDrawable, null);
			title.setContentDescription(isBookmarked
					? context.getString(R.string.in_bookmarks_content_description, event.getTitle())
					: null
			);
			String personsSummary = event.getPersonsSummary();
			persons.setText(personsSummary);
			persons.setVisibility(TextUtils.isEmpty(personsSummary) ? View.GONE : View.VISIBLE);
			Track track = event.getTrack();
			trackName.setText(track.getName());
			trackName.setTextColor(ContextCompat.getColorStateList(context, track.getType().getTextColorResId()));
			trackName.setContentDescription(context.getString(R.string.track_content_description, track.getName()));
		}

		void bindDetails(@NonNull Event event, boolean showDay, @Nullable RoomStatus roomStatus) {
			Context context = details.getContext();
			Date startTime = event.getStartTime();
			Date endTime = event.getEndTime();
			String startTimeString = (startTime != null) ? timeDateFormat.format(startTime) : "?";
			String endTimeString = (endTime != null) ? timeDateFormat.format(endTime) : "?";
			String roomName = event.getRoomName();
			CharSequence detailsText;
			if (showDay) {
				detailsText = String.format("%1$s, %2$s ― %3$s  |  %4$s", event.getDay().getShortName(), startTimeString, endTimeString, roomName);
			} else {
				detailsText = String.format("%1$s ― %2$s  |  %3$s", startTimeString, endTimeString, roomName);
			}
			CharSequence detailsDescription = detailsText;
			if (roomStatus != null) {
				SpannableString detailsSpannable = new SpannableString(detailsText);
				int color = ContextCompat.getColor(context, roomStatus.getColorResId());
				detailsSpannable.setSpan(new ForegroundColorSpan(color),
						detailsText.length() - roomName.length(),
						detailsText.length(),
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				detailsText = detailsSpannable;

				detailsDescription = String.format("%1$s (%2$s)", detailsDescription, context.getString(roomStatus.getNameResId()));
			}
			details.setText(detailsText);
			details.setContentDescription(context.getString(R.string.details_content_description, detailsDescription));
		}

		@Override
		public void onClick(View view) {
			if (event != null) {
				Context context = view.getContext();
				Intent intent = new Intent(context, EventDetailsActivity.class)
						.putExtra(EventDetailsActivity.EXTRA_EVENT, event);
				context.startActivity(intent);
			}
		}
	}
}
