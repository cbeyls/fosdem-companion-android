package be.digitalia.fosdem.livedata;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class LiveDataFactory {

	static final Handler handler = new Handler(Looper.getMainLooper());

	private LiveDataFactory() {
	}

	private static class IntervalLiveData extends LiveData<Long> implements Runnable {

		private final long periodInMillis;
		private long updateTime = 0L;
		private long version = 0L;

		IntervalLiveData(long periodInMillis) {
			this.periodInMillis = periodInMillis;
		}

		@Override
		protected void onActive() {
			final long now = SystemClock.elapsedRealtime();
			if (now >= updateTime) {
				update(now);
			} else {
				handler.postDelayed(this, updateTime - now);
			}
		}

		@Override
		protected void onInactive() {
			handler.removeCallbacks(this);
		}

		private void update(long now) {
			setValue(version++);
			updateTime = now + periodInMillis;
			handler.postDelayed(this, periodInMillis);
		}

		@Override
		public void run() {
			update(SystemClock.elapsedRealtime());
		}
	}

	public static LiveData<Long> interval(long period, @NonNull TimeUnit unit) {
		return new IntervalLiveData(unit.toMillis(period));
	}

	private static class SchedulerLiveData extends LiveData<Boolean> implements Runnable {

		private final long[] startEndTimestamps;
		private int nowPosition = -1;

		SchedulerLiveData(long[] startEndTimestamps) {
			this.startEndTimestamps = startEndTimestamps;
		}

		@Override
		protected void onActive() {
			final long now = System.currentTimeMillis();
			updateState(now, Arrays.binarySearch(startEndTimestamps, now));
		}

		@Override
		protected void onInactive() {
			handler.removeCallbacks(this);
		}

		@Override
		public void run() {
			final int position = nowPosition;
			updateState(startEndTimestamps[position], position);
		}

		private void updateState(long now, int position) {
			final int size = startEndTimestamps.length;
			if (position >= 0) {
				do {
					position++;
				} while (position < size && startEndTimestamps[position] == now);
			} else {
				position = ~position;
			}
			final Boolean isOn = position % 2 != 0;
			if (getValue() != isOn) {
				setValue(isOn);
			}
			if (position < size) {
				nowPosition = position;
				handler.postDelayed(this, startEndTimestamps[position] - now);
			}
		}
	}

	/**
	 * Builds a LiveData whose value is true during scheduled periods.
	 *
	 * @param startEndTimestamps a list of timestamps in milliseconds, sorted in chronological order.
	 *                           Odd and even values represent beginnings and ends of periods, respectively.
	 */
	public static LiveData<Boolean> scheduler(long... startEndTimestamps) {
		return new SchedulerLiveData(startEndTimestamps);
	}
}
