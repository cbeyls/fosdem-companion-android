package be.digitalia.fosdem.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
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
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.utils.DateUtils;

public class TrackScheduleAdapter extends RecyclerViewCursorAdapter<TrackScheduleAdapter.ViewHolder> {

	public interface EventClickListener {
		void onEventClick(int position, Event event);
	}

	private static final Object TIME_COLORS_PAYLOAD = new Object();
	private static final Object SELECTION_PAYLOAD = new Object();

	private final LayoutInflater inflater;
	private final DateFormat timeDateFormat;
	private final int timeBackgroundColor;
	private final int timeForegroundColor;
	private final int timeRunningBackgroundColor;
	private final int timeRunningForegroundColor;
	@Nullable
	private final EventClickListener listener;

	private long currentTime = -1L;
	private long selectedId = -1L;

	public TrackScheduleAdapter(Context context, @Nullable EventClickListener listener) {
		inflater = LayoutInflater.from(context);
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
	public Event getItem(int position) {
		return DatabaseManager.toEvent((Cursor) super.getItem(position));
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.item_schedule_event, parent, false);
		return new ViewHolder(view, R.drawable.activated_background, listener);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
		Context context = holder.itemView.getContext();
		Event event = DatabaseManager.toEvent(cursor, holder.event);
		holder.event = event;

		holder.time.setText(timeDateFormat.format(event.getStartTime()));
		bindTimeColors(holder, event);

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
		holder.room.setText(event.getRoomName());
		holder.room.setContentDescription(context.getString(R.string.room_content_description, event.getRoomName()));

		bindSelection(holder, event);
	}

	private void bindTimeColors(ViewHolder holder, Event event) {
		final TextView timeTextView = holder.time;
		if ((currentTime != -1L) && event.isRunningAtTime(currentTime)) {
			// Contrast colors for running event
			timeTextView.setBackgroundColor(timeRunningBackgroundColor);
			timeTextView.setTextColor(timeRunningForegroundColor);
			timeTextView.setContentDescription(timeTextView.getContext().getString(R.string.in_progress_content_description, timeTextView.getText()));
		} else {
			// Normal colors
			timeTextView.setBackgroundColor(timeBackgroundColor);
			timeTextView.setTextColor(timeForegroundColor);
			// Use text as content description
			timeTextView.setContentDescription(null);
		}
	}

	private void bindSelection(ViewHolder holder, Event event) {
		holder.itemView.setActivated(event.getId() == selectedId);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (payloads.isEmpty()) {
			onBindViewHolder(holder, position);
		} else {
			if (holder.event != null) {
				if (payloads.contains(TIME_COLORS_PAYLOAD)) {
					bindTimeColors(holder, holder.event);
				}
				if (payloads.contains(SELECTION_PAYLOAD)) {
					bindSelection(holder, holder.event);
				}
			}
		}
	}

	static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		TextView time;
		TextView title;
		TextView persons;
		TextView room;
		@Nullable
		EventClickListener listener;

		Event event;

		public ViewHolder(@NonNull View itemView, @DrawableRes int activatedBackgroundResId, @Nullable EventClickListener listener) {
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
					ViewCompat.setBackground(itemView, null);
					itemView.setBackgroundResource(0);
					newBackground = new LayerDrawable(new Drawable[]{existingBackground, activatedBackground});
				}
				ViewCompat.setBackground(itemView, newBackground);
			}
			this.listener = listener;
		}

		@Override
		public void onClick(View v) {
			if (listener != null) {
				listener.onEventClick(getAdapterPosition(), event);
			}
		}
	}
}
