package be.digitalia.fosdem.api;

import android.content.Context;
import android.content.Intent;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.DetailedEvent;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.parsers.EventsParser;
import be.digitalia.fosdem.utils.HttpUtils;

/**
 * Main API entry point.
 *
 * @author Christophe Beyls
 */
public class FosdemApi {

	// Local broadcasts parameters
	public static final String ACTION_DOWNLOAD_SCHEDULE_RESULT = BuildConfig.APPLICATION_ID + ".action.DOWNLOAD_SCHEDULE_RESULT";
	public static final String EXTRA_RESULT = "RESULT";

	public static final int RESULT_ERROR = -1;
	public static final int RESULT_UP_TO_DATE = -2;

	private static final Lock scheduleLock = new ReentrantLock();
	private static final MutableLiveData<Integer> progress = new MutableLiveData<>();
	private static LiveData<Map<String, RoomStatus>> roomStatuses;

	/**
	 * Download & store the schedule to the database.
	 * Only one thread at a time will perform the actual action, the other ones will return immediately.
	 * The result will be sent back in the form of a local broadcast with an ACTION_DOWNLOAD_SCHEDULE_RESULT action.
	 */
	@WorkerThread
	public static void downloadSchedule(Context context) {
		if (!scheduleLock.tryLock()) {
			// If a download is already in progress, return immediately
			return;
		}

		progress.postValue(-1);
		int result = RESULT_ERROR;
		try {
			DatabaseManager dbManager = DatabaseManager.getInstance();
			HttpUtils.HttpResult httpResult = HttpUtils.get(
					FosdemUrls.getSchedule(),
					dbManager.getLastModifiedTag(),
					new HttpUtils.ProgressUpdateListener() {
						@Override
						public void onProgressUpdate(int percent) {
							progress.postValue(percent);
						}
					});
			if (httpResult.inputStream == null) {
				// Nothing to parse, the result is up-to-date.
				result = RESULT_UP_TO_DATE;
				return;
			}

			try {
				Iterable<DetailedEvent> events = new EventsParser().parse(httpResult.inputStream);
				result = dbManager.storeSchedule(events, httpResult.lastModified);
			} finally {
				try {
					httpResult.inputStream.close();
				} catch (Exception ignored) {
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			progress.postValue(100);
			Intent resultIntent = new Intent(ACTION_DOWNLOAD_SCHEDULE_RESULT)
					.putExtra(EXTRA_RESULT, result);
			LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent);
			scheduleLock.unlock();
		}
	}

	/**
	 * @return The current schedule download progress:
	 * -1   : in progress, indeterminate
	 * 0..99: progress value
	 * 100  : download complete or inactive
	 */
	public static LiveData<Integer> getDownloadScheduleProgress() {
		return progress;
	}

	@MainThread
	public static LiveData<Map<String, RoomStatus>> getRoomStatuses(Context context) {
		if (roomStatuses == null) {
			// The room statuses will only be loaded when the event is live.
			// RoomStatusesLiveData uses the days from the database to determine it.
			roomStatuses = new RoomStatusesLiveData(AppDatabase.getInstance(context).getScheduleDao().getDays());
			// Implementors: replace the above line with the next one to disable room status support
			// roomStatuses = new MutableLiveData<>();
		}
		return roomStatuses;
	}
}
