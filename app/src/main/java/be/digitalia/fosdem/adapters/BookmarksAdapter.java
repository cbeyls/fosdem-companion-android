package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.savedstate.SavedStateRegistryOwner;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.DateUtils;
import be.digitalia.fosdem.widgets.MultiChoiceHelper;

public class BookmarksAdapter extends ListAdapter<Event, BookmarksAdapter.ViewHolder>
		implements Observer<Map<String, RoomStatus>> {

	private static final DiffUtil.ItemCallback<Event> DIFF_CALLBACK = new SimpleItemCallback<Event>() {
		@Override
		public boolean areContentsTheSame(@NonNull Event oldEvent, @NonNull Event newEvent) {
			return ObjectsCompat.equals(oldEvent.getTitle(), newEvent.getTitle())
					&& ObjectsCompat.equals(oldEvent.getPersonsSummary(), newEvent.getPersonsSummary())
					&& ObjectsCompat.equals(oldEvent.getTrack(), newEvent.getTrack())
					&& ObjectsCompat.equals(oldEvent.getDay(), newEvent.getDay())
					&& ObjectsCompat.equals(oldEvent.getStartTime(), newEvent.getStartTime())
					&& ObjectsCompat.equals(oldEvent.getEndTime(), newEvent.getEndTime())
					&& ObjectsCompat.equals(oldEvent.getRoomName(), newEvent.getRoomName());
		}
	};
	static final Object DETAILS_PAYLOAD = new Object();

	private final DateFormat timeDateFormat;
	@ColorInt
	private final int errorColor;
	private final SimpleArrayMap<RecyclerView.AdapterDataObserver, BookmarksDataObserverWrapper> observers = new SimpleArrayMap<>();
	final MultiChoiceHelper multiChoiceHelper;
	private Map<String, RoomStatus> roomStatuses;

	public BookmarksAdapter(@NonNull AppCompatActivity activity, @NonNull SavedStateRegistryOwner owner,
							@NonNull MultiChoiceHelper.MultiChoiceModeListener multiChoiceModeListener) {
		super(DIFF_CALLBACK);
		setHasStableIds(true);
		timeDateFormat = DateUtils.getTimeDateFormat(activity);
		TypedArray a = activity.getTheme().obtainStyledAttributes(R.styleable.ErrorColors);
		errorColor = a.getColor(R.styleable.ErrorColors_colorError, 0);
		a.recycle();

		multiChoiceHelper = new MultiChoiceHelper(activity, owner, this);
		multiChoiceHelper.setMultiChoiceModeListener(multiChoiceModeListener);

		FosdemApi.getRoomStatuses(activity).observe(owner, this);
	}

	@NonNull
	public MultiChoiceHelper getMultiChoiceHelper() {
		return multiChoiceHelper;
	}

	@Override
	public void onChanged(@Nullable Map<String, RoomStatus> roomStatuses) {
		this.roomStatuses = roomStatuses;
		notifyItemRangeChanged(0, getItemCount(), DETAILS_PAYLOAD);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
		return new ViewHolder(view, multiChoiceHelper, timeDateFormat, errorColor);
	}

	private RoomStatus getRoomStatus(Event event) {
		return (roomStatuses == null) ? null : roomStatuses.get(event.getRoomName());
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		final Event event = getItem(position);
		holder.bind(event);
		final Event previous = position > 0 ? getItem(position - 1) : null;
		final Event next = position + 1 < getItemCount() ? getItem(position + 1) : null;
		holder.bindDetails(event, previous, next, getRoomStatus(event));
		holder.bindSelection();
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (payloads.isEmpty()) {
			onBindViewHolder(holder, position);
		} else {
			final Event event = getItem(position);
			if (payloads.contains(DETAILS_PAYLOAD)) {
				final Event previous = position > 0 ? getItem(position - 1) : null;
				final Event next = position + 1 < getItemCount() ? getItem(position + 1) : null;
				holder.bindDetails(event, previous, next, getRoomStatus(event));
			}
			if (payloads.contains(MultiChoiceHelper.SELECTION_PAYLOAD)) {
				holder.bindSelection();
			}
		}
	}

	@Override
	public void registerAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
		if (!observers.containsKey(observer)) {
			final BookmarksDataObserverWrapper wrapper = new BookmarksDataObserverWrapper(observer, this);
			observers.put(observer, wrapper);
			super.registerAdapterDataObserver(wrapper);
		}
	}

	@Override
	public void unregisterAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
		final BookmarksDataObserverWrapper wrapper = observers.remove(observer);
		if (wrapper != null) {
			super.unregisterAdapterDataObserver(wrapper);
		}
	}

	static class ViewHolder extends MultiChoiceHelper.ViewHolder implements View.OnClickListener {
		final TextView title;
		final TextView persons;
		final TextView trackName;
		final TextView details;

		private final DateFormat timeDateFormat;
		@ColorInt
		private final int errorColor;

		Event event;

		public ViewHolder(@NonNull View itemView, @NonNull MultiChoiceHelper helper,
						  @NonNull DateFormat timeDateFormat, @ColorInt int errorColor) {
			super(itemView, helper);

			title = itemView.findViewById(R.id.title);
			persons = itemView.findViewById(R.id.persons);
			trackName = itemView.findViewById(R.id.track_name);
			details = itemView.findViewById(R.id.details);
			setOnClickListener(this);

			this.timeDateFormat = timeDateFormat;
			this.errorColor = errorColor;
		}

		void bind(@NonNull Event event) {
			Context context = itemView.getContext();
			this.event = event;

			title.setText(event.getTitle());
			String personsSummary = event.getPersonsSummary();
			persons.setText(personsSummary);
			persons.setVisibility(TextUtils.isEmpty(personsSummary) ? View.GONE : View.VISIBLE);
			Track track = event.getTrack();
			trackName.setText(track.getName());
			trackName.setTextColor(ContextCompat.getColorStateList(context, track.getType().getTextColorResId()));
			trackName.setContentDescription(context.getString(R.string.track_content_description, track.getName()));
		}

		void bindDetails(@NonNull Event event, @Nullable Event previous, @Nullable Event next, @Nullable RoomStatus roomStatus) {
			Context context = details.getContext();
			Date startTime = event.getStartTime();
			Date endTime = event.getEndTime();
			String startTimeString = (startTime != null) ? timeDateFormat.format(startTime) : "?";
			String endTimeString = (endTime != null) ? timeDateFormat.format(endTime) : "?";
			String roomName = event.getRoomName();
			String detailsText = String.format("%1$s, %2$s ― %3$s  |  %4$s", event.getDay().getShortName(), startTimeString, endTimeString, roomName);
			SpannableString detailsSpannable = new SpannableString(detailsText);
			CharSequence detailsDescription = detailsText;

			// Highlight the date and time with error color in case of conflicting schedules
			if (isOverlapping(event, previous, next)) {
				int endPosition = detailsText.indexOf(" | ");
				detailsSpannable.setSpan(new ForegroundColorSpan(errorColor), 0, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				detailsSpannable.setSpan(new StyleSpan(Typeface.BOLD), 0, endPosition, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				detailsDescription = context.getString(R.string.bookmark_conflict_content_description, detailsDescription);
			}
			if (roomStatus != null) {
				int color = ContextCompat.getColor(context, roomStatus.getColorResId());
				detailsSpannable.setSpan(new ForegroundColorSpan(color),
						detailsText.length() - roomName.length(),
						detailsText.length(),
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			details.setText(detailsSpannable);
			details.setContentDescription(context.getString(R.string.details_content_description, detailsDescription));
		}

		/**
		 * Checks if the current event is overlapping with the previous or next one.
		 */
		private static boolean isOverlapping(@NonNull Event event, @Nullable Event previous, @Nullable Event next) {
			final Date startTime = event.getStartTime();
			final Date previousEndTime = (previous == null) ? null : previous.getEndTime();
			if (startTime != null && previousEndTime != null && previousEndTime.getTime() > startTime.getTime()) {
				// The event overlaps with the previous one
				return true;
			}

			final Date endTime = event.getEndTime();
			final Date nextStartTime = (next == null) ? null : next.getStartTime();
			// The event overlaps with the next one
			return endTime != null && nextStartTime != null && nextStartTime.getTime() < endTime.getTime();
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

	/**
	 * An observer dispatching updates to the source observer while additionally notifying changes
	 * of the immediately previous and next items in order to properly update their overlapping status display.
	 */
	static class BookmarksDataObserverWrapper extends RecyclerView.AdapterDataObserver {
		private final RecyclerView.AdapterDataObserver observer;
		private final RecyclerView.Adapter<?> adapter;

		public BookmarksDataObserverWrapper(RecyclerView.AdapterDataObserver observer, RecyclerView.Adapter<?> adapter) {
			this.observer = observer;
			this.adapter = adapter;
		}

		private void updatePrevious(int position) {
			if (position >= 0) {
				observer.onItemRangeChanged(position, 1, DETAILS_PAYLOAD);
			}
		}

		private void updateNext(int position) {
			if (position < adapter.getItemCount()) {
				observer.onItemRangeChanged(position, 1, DETAILS_PAYLOAD);
			}
		}

		@Override
		public void onChanged() {
			observer.onChanged();
		}

		@Override
		public void onItemRangeChanged(int positionStart, int itemCount) {
			observer.onItemRangeChanged(positionStart, itemCount);
			updatePrevious(positionStart - 1);
			updateNext(positionStart + itemCount);
		}

		@Override
		public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
			observer.onItemRangeChanged(positionStart, itemCount, payload);
			updatePrevious(positionStart - 1);
			updateNext(positionStart + itemCount);
		}

		@Override
		public void onItemRangeInserted(int positionStart, int itemCount) {
			observer.onItemRangeInserted(positionStart, itemCount);
			updatePrevious(positionStart - 1);
			updateNext(positionStart + itemCount);
		}

		@Override
		public void onItemRangeRemoved(int positionStart, int itemCount) {
			observer.onItemRangeRemoved(positionStart, itemCount);
			updatePrevious(positionStart - 1);
			updateNext(positionStart);
		}

		@Override
		public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
			updatePrevious(fromPosition - 1);
			updateNext(fromPosition + itemCount);
			observer.onItemRangeMoved(fromPosition, toPosition, itemCount);
			updatePrevious(toPosition - 1);
			updateNext(toPosition + itemCount);
		}
	}
}
