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
}
