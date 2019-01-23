package be.digitalia.fosdem.api;

import android.annotation.SuppressLint;
import android.os.*;
import android.text.format.DateUtils;
import androidx.lifecycle.LiveData;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.parsers.RoomStatusesParser;
import be.digitalia.fosdem.utils.HttpUtils;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Loads and maintain the Room statuses live during the event.
 */
class LiveRoomStatusesLiveData extends LiveData<Map<String, RoomStatus>> {

	private static final long REFRESH_DELAY = 90L * DateUtils.SECOND_IN_MILLIS;
	private static final long FIRST_ERROR_REFRESH_DELAY = 30L * DateUtils.SECOND_IN_MILLIS;
	private static final long EXPIRATION_DELAY = 6L * DateUtils.MINUTE_IN_MILLIS;

	private static final int EXPIRE_WHAT = 0;
	private static final int REFRESH_WHAT = 1;

	private final Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
				case EXPIRE_WHAT:
					expire();
					return true;
				case REFRESH_WHAT:
					refresh();
					return true;
			}
			return false;
		}
	});

	private long expirationTime = Long.MAX_VALUE;
	private long nextRefreshTime = 0L;
	private int retryAttempt = 0;
	private AsyncTask<Void, Void, Map<String, RoomStatus>> currentTask = null;

	@Override
	protected void onActive() {
		long now = SystemClock.elapsedRealtime();
		if (expirationTime != Long.MAX_VALUE) {
			if (now < expirationTime) {
				handler.sendEmptyMessageDelayed(EXPIRE_WHAT, expirationTime - now);
			} else {
				expire();
			}
		}
		if (now < nextRefreshTime) {
			handler.sendEmptyMessageDelayed(REFRESH_WHAT, nextRefreshTime - now);
		} else {
			refresh();
		}
	}

	@Override
	protected void onInactive() {
		handler.removeMessages(EXPIRE_WHAT);
		handler.removeMessages(REFRESH_WHAT);
	}

	@SuppressLint("StaticFieldLeak")
	void refresh() {
		if (currentTask != null) {
			// Let the ongoing task complete with success or error
			return;
		}
		currentTask = new AsyncTask<Void, Void, Map<String, RoomStatus>>() {

			@Override
			protected Map<String, RoomStatus> doInBackground(Void... voids) {
				try {
					InputStream is = HttpUtils.get(FosdemUrls.getRooms());
					try {
						return new RoomStatusesParser().parse(is);
					} finally {
						is.close();
					}
				} catch (Throwable e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(Map<String, RoomStatus> result) {
				currentTask = null;
				if (result != null) {
					onSuccess(result);
				} else {
					onError();
				}
			}
		};
		currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	void onSuccess(Map<String, RoomStatus> result) {
		setValue(result);
		retryAttempt = 0;
		long now = SystemClock.elapsedRealtime();
		expirationTime = now + EXPIRATION_DELAY;
		if (hasActiveObservers()) {
			handler.sendEmptyMessageDelayed(EXPIRE_WHAT, EXPIRATION_DELAY);
		}
		scheduleNextRefresh(now, REFRESH_DELAY);
	}

	void onError() {
		// Use exponential backoff for retries
		long multiplier = (long) Math.pow(2, retryAttempt);
		retryAttempt++;
		scheduleNextRefresh(SystemClock.elapsedRealtime(),
				Math.min(FIRST_ERROR_REFRESH_DELAY * multiplier, REFRESH_DELAY));
	}

	private void scheduleNextRefresh(long now, long delay) {
		nextRefreshTime = now + delay;
		if (hasActiveObservers()) {
			handler.sendEmptyMessageDelayed(REFRESH_WHAT, delay);
		}
	}

	void expire() {
		// When the data expires, replace it with an empty value
		setValue(Collections.<String, RoomStatus>emptyMap());
		expirationTime = Long.MAX_VALUE;
	}
}
