package be.digitalia.fosdem.api;

import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.RoomStatus;

class RoomStatusesLiveData extends MediatorLiveData<Map<String, RoomStatus>> {

	// 8:30 (local time)
	private static final long DAY_START_TIME = 8 * DateUtils.HOUR_IN_MILLIS + 30 * DateUtils.MINUTE_IN_MILLIS;
	// 19:00 (local time)
	private static final long DAY_END_TIME = 19 * DateUtils.HOUR_IN_MILLIS;

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable updateRunnable = new Runnable() {
		@Override
		public void run() {
			updateStrategy();
		}
	};

	private final LiveData<Map<String, RoomStatus>> liveRoomStatuses = new LiveRoomStatusesLiveData();
	List<Day> days = null;
	private boolean isLive = false;

	RoomStatusesLiveData(LiveData<List<Day>> daysLiveData) {
		addSource(daysLiveData, new Observer<List<Day>>() {
			@Override
			public void onChanged(@Nullable List<Day> days) {
				RoomStatusesLiveData.this.days = days;
				updateStrategy();
			}
		});
	}

	@Override
	protected void onActive() {
		super.onActive();
		updateStrategy();
	}

	@Override
	protected void onInactive() {
		super.onInactive();
		handler.removeCallbacks(updateRunnable);
	}

	void updateStrategy() {
		handler.removeCallbacks(updateRunnable);
		if (days == null) {
			return;
		}
		long now = System.currentTimeMillis();
		for (int i = 0, size = days.size(); i < size; ++i) {
			long date = days.get(i).getDate().getTime();
			long startTime = date + DAY_START_TIME;
			if (now < startTime) {
				setLive(false);
				handler.postDelayed(updateRunnable, startTime - now);
				return;
			}
			long endTime = date + DAY_END_TIME;
			if (now < endTime) {
				setLive(true);
				handler.postDelayed(updateRunnable, endTime - now);
				return;
			}
		}
		// All days are in the past, no next update to schedule
		setLive(false);
	}

	private void setLive(boolean isLive) {
		if (this.isLive != isLive) {
			if (isLive) {
				// Event is live, connect to the LiveData providing live values
				addSource(liveRoomStatuses, new Observer<Map<String, RoomStatus>>() {
					@Override
					public void onChanged(@Nullable Map<String, RoomStatus> value) {
						setValue(value);
					}
				});
			} else {
				// Event is offline, provide empty values
				removeSource(liveRoomStatuses);
				setValue(Collections.<String, RoomStatus>emptyMap());
			}
			this.isLive = isLive;
		}
	}
}
