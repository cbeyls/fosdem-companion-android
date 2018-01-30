package be.digitalia.fosdem.api;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateUtils;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import be.digitalia.fosdem.livedata.AsyncTaskLiveData;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.parsers.RoomStatusesParser;
import be.digitalia.fosdem.utils.HttpUtils;

/**
 * Loads and maintain the Room statuses live during the event.
 */
class LiveRoomStatusesLiveData extends AsyncTaskLiveData<Map<String, RoomStatus>> {

	private static final long REFRESH_DELAY = 90L * DateUtils.SECOND_IN_MILLIS;
	private static final long FIRST_ERROR_REFRESH_DELAY = 30L * DateUtils.SECOND_IN_MILLIS;
	private static final long EXPIRATION_DELAY = 6L * DateUtils.MINUTE_IN_MILLIS;

	private final Handler handler = new Handler(Looper.getMainLooper());

	private final Runnable expirationRunnable = new Runnable() {
		@Override
		public void run() {
			expire();
		}
	};
	private long expirationTime = Long.MAX_VALUE;

	private final Runnable refreshRunnable = new Runnable() {
		@Override
		public void run() {
			forceLoad();
		}
	};
	private long nextRefreshTime = 0L;
	private int retryAttempt = 0;

	@Override
	protected void onActive() {
		long now = SystemClock.elapsedRealtime();
		if (expirationTime != Long.MAX_VALUE) {
			if (now < expirationTime) {
				handler.postDelayed(expirationRunnable, expirationTime - now);
			} else {
				expire();
			}
		}
		if (now < nextRefreshTime) {
			handler.postDelayed(refreshRunnable, nextRefreshTime - now);
		} else {
			forceLoad();
		}
	}

	@Override
	protected void onInactive() {
		handler.removeCallbacks(expirationRunnable);
		handler.removeCallbacks(refreshRunnable);
	}

	@Override
	protected Map<String, RoomStatus> loadInBackground() throws Exception {
		InputStream is = HttpUtils.get(FosdemUrls.getRooms());
		try {
			return new RoomStatusesParser().parse(is);
		} finally {
			is.close();
		}
	}

	@Override
	protected void onSuccess(Map<String, RoomStatus> result) {
		setValue(result);
		retryAttempt = 0;
		long now = SystemClock.elapsedRealtime();
		expirationTime = now + EXPIRATION_DELAY;
		if (hasActiveObservers()) {
			handler.postDelayed(expirationRunnable, EXPIRATION_DELAY);
		}
		scheduleNextRefresh(now, REFRESH_DELAY);
	}

	@Override
	protected void onError(Throwable error) {
		// Use exponential backoff for retries
		long multiplier = (long) Math.pow(2, retryAttempt);
		retryAttempt++;
		scheduleNextRefresh(SystemClock.elapsedRealtime(),
				Math.min(FIRST_ERROR_REFRESH_DELAY * multiplier, REFRESH_DELAY));
	}

	private void scheduleNextRefresh(long now, long delay) {
		nextRefreshTime = now + delay;
		if (hasActiveObservers()) {
			handler.postDelayed(refreshRunnable, delay);
		}
	}

	void expire() {
		// When the data expires, replace it with an empty value
		setValue(Collections.<String, RoomStatus>emptyMap());
		expirationTime = Long.MAX_VALUE;
	}
}
