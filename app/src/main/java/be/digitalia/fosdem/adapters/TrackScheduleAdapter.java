package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.utils.DateUtils;

public class TrackScheduleAdapter extends ListAdapter<StatusEvent, TrackScheduleAdapter.ViewHolder> {

	public interface EventClickListener {
		void onEventClick(int position, Event event);
	}

	private static final DiffUtil.ItemCallback<StatusEvent> DIFF_CALLBACK = new SimpleItemCallback<StatusEvent>() {
		@Override
		public boolean areContentsTheSame(@NonNull StatusEvent oldItem, @NonNull StatusEvent newItem) {
			final Event oldEvent = oldItem.getEvent();
			final Event newEvent = newItem.getEvent();
			return ObjectsCompat.equals(oldEvent.getTitle(), newEvent.getTitle())
					&& ObjectsCompat.equals(oldEvent.getPersonsSummary(), newEvent.getPersonsSummary())
					&& ObjectsCompat.equals(oldEvent.getRoomName(), newEvent.getRoomName())
					&& ObjectsCompat.equals(oldEvent.getStartTime(), newEvent.getStartTime())
					&& oldItem.isBookmarked() == newItem.isBookmarked();
		}
	};
	private static final Object TIME_COLORS_PAYLOAD = new Object();
	private static final Object SELECTION_PAYLOAD = new Object();

	final DateFormat timeDateFormat;
	final int timeBackgroundColor;
	final int timeForegroundColor;
	final int timeRunningBackgroundColor;
	final int timeRunningForegroundColor;
	@Nullable
	final EventClickListener listener;

	private long currentTime = -1L;
	private long selectedId = -1L;

	public TrackScheduleAdapter(Context context, @Nullable EventClickListener listener) {
		super(DIFF_CALLBACK);
		setHasStableIds(true);
		timeDateFormat = DateUtils.getTimeDateFormat(context);
		timeBackgroundColor = ContextCompat.getColor(context, R.color.schedule_time_background);
		timeRunningBackgroundColor = ContextCompat.getColor(context, R.color.schedule_time_running_background);

		TypedArray a = context.getTheme().obtainStyledAttributes(R.styleable.PrimaryTextColors);
		timeForegroundColor = a.getColor(R.styleable.PrimaryTextColors_android_textColorPrimary, 0);
		timeRunningForegroundColor = a.getColor(R.styleable.PrimaryTextColors_android_textColorPrimaryInverse, 0);
		a.recycle();

		this.listener = listener;
	}

	/**
	 * @return The position of the item id in the current data set, or -1 if not found.
	 */
	public int getPositionForId(long id) {
		if (id != -1) {
			final int count = getItemCount();
			for (int i = 0; i < count; ++i) {
				if (getItemId(i) == id) {
					return i;
				}
			}
		}
		return RecyclerView.NO_POSITION;
	}

	public void setCurrentTime(long time) {
		if (currentTime != time) {
			currentTime = time;
			notifyItemRangeChanged(0, getItemCount(), TIME_COLORS_PAYLOAD);
		}
	}

	public void setSelectedId(long newId) {
		final long oldId = selectedId;
		if (oldId != newId) {
			selectedId = newId;
			final int count = getItemCount();
			for (int i = 0; i < count; ++i) {
				final long id = getItemId(i);
				if (id == oldId || id == newId) {
					notifyItemChanged(i, SELECTION_PAYLOAD);
				}
			}
		}
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getEvent().getId();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_event, parent, false);
		return new ViewHolder(view, R.drawable.activated_background);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		final StatusEvent statusEvent = getItem(position);
		final Event event = statusEvent.getEvent();
		holder.bind(event, statusEvent.isBookmarked());
		holder.bindTimeColors(event, currentTime);
		holder.bindSelection(event.getId() == selectedId);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (payloads.isEmpty()) {
			onBindViewHolder(holder, position);
		} else {
			final StatusEvent statusEvent = getItem(position);
			if (payloads.contains(TIME_COLORS_PAYLOAD)) {
				holder.bindTimeColors(statusEvent.getEvent(), currentTime);
			}
			if (payloads.contains(SELECTION_PAYLOAD)) {
				holder.bindSelection(statusEvent.getEvent().getId() == selectedId);
			}
		}
	}

	class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		final TextView time;
		final TextView title;
		final TextView persons;
		final TextView room;

		Event event;

		ViewHolder(@NonNull View itemView, @DrawableRes int activatedBackgroundResId) {
			super(itemView);
			time = itemView.findViewById(R.id.time);
			title = itemView.findViewById(R.id.title);
			persons = itemView.findViewById(R.id.persons);
			room = itemView.findViewById(R.id.room);
			itemView.setOnClickListener(this);
			if (activatedBackgroundResId != 0) {
				// Compose a new background drawable by combining the existing one with the activated background
				final Drawable existingBackground = itemView.getBackground();
				final Drawable activatedBackground = ContextCompat.getDrawable(itemView.getContext(), activatedBackgroundResId);
				Drawable newBackground;
				if (existingBackground == null) {
					newBackground = activatedBackground;
				} else {
					// Clear the existing background drawable callback so it can be assigned to the LayerDrawable
					itemView.setBackground(null);
					newBackground = new LayerDrawable(new Drawable[]{existingBackground, activatedBackground});
				}
				itemView.setBackground(newBackground);
			}
		}

		void bind(@NonNull Event event, boolean isBookmarked) {
			Context context = itemView.getContext();
			this.event = event;

			time.setText(timeDateFormat.format(event.getStartTime()));
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
			room.setText(event.getRoomName());
			room.setContentDescription(context.getString(R.string.room_content_description, event.getRoomName()));
		}

		void bindTimeColors(@NonNull Event event, long currentTime) {
			if ((currentTime != -1L) && event.isRunningAtTime(currentTime)) {
				// Contrast colors for running event
				time.setBackgroundColor(timeRunningBackgroundColor);
				time.setTextColor(timeRunningForegroundColor);
				time.setContentDescription(time.getContext().getString(R.string.in_progress_content_description, time.getText()));
			} else {
				// Normal colors
				time.setBackgroundColor(timeBackgroundColor);
				time.setTextColor(timeForegroundColor);
				// Use text as content description
				time.setContentDescription(null);
			}
		}

		void bindSelection(boolean isSelected) {
			itemView.setActivated(isSelected);
		}

		@Override
		public void onClick(View v) {
			if (listener != null) {
				listener.onEventClick(getAdapterPosition(), event);
			}
		}
	}
}
