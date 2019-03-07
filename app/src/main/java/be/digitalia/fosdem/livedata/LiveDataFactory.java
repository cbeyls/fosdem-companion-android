package be.digitalia.fosdem.livedata;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

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
			if (version == 0L) {
				run();
			} else {
				final long now = SystemClock.elapsedRealtime();
				if (now >= updateTime) {
					handler.post(this);
				} else {
					handler.postDelayed(this, updateTime - now);
				}
			}
		}

		@Override
		protected void onInactive() {
			handler.removeCallbacks(this);
		}

		@Override
		public void run() {
			setValue(version++);
			updateTime = SystemClock.elapsedRealtime() + periodInMillis;
			handler.postDelayed(this, periodInMillis);
		}
	}

	public static LiveData<Long> interval(long period, @NonNull TimeUnit unit) {
		return new IntervalLiveData(unit.toMillis(period));
	}
}
