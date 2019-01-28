package be.digitalia.fosdem.api;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.db.ScheduleDao;
import be.digitalia.fosdem.livedata.SingleEvent;
import be.digitalia.fosdem.model.DetailedEvent;
import be.digitalia.fosdem.model.DownloadScheduleResult;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.parsers.EventsParser;
import be.digitalia.fosdem.utils.HttpUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main API entry point.
 *
 * @author Christophe Beyls
 */
public class FosdemApi {

	private static final AtomicBoolean isLoading = new AtomicBoolean();
	private static final MutableLiveData<Integer> progress = new MutableLiveData<>();
	private static final MutableLiveData<SingleEvent<DownloadScheduleResult>> result = new MutableLiveData<>();
	private static LiveData<Map<String, RoomStatus>> roomStatuses;

	/**
	 * Download & store the schedule to the database.
	 * Only one thread at a time will perform the actual action, the other ones will return immediately.
	 * The result will be sent back in the consumable Result LiveData.
	 */
	@MainThread
	public static void downloadSchedule(Context context) {
		if (!isLoading.compareAndSet(false, true)) {
			// If a download is already in progress, return immediately
			return;
		}
		final Context appContext = context.getApplicationContext();
		AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
			@Override
			public void run() {
				downloadScheduleInternal(appContext);
				isLoading.set(false);
			}
		});
	}

	@WorkerThread
	private static void downloadScheduleInternal(Context context) {
		progress.postValue(-1);
		DownloadScheduleResult res = DownloadScheduleResult.error();
		try {
			ScheduleDao scheduleDao = AppDatabase.getInstance(context).getScheduleDao();
			HttpUtils.HttpResult httpResult = HttpUtils.get(
					FosdemUrls.getSchedule(),
					scheduleDao.getLastModifiedTag(),
					new HttpUtils.ProgressUpdateListener() {
						@Override
						public void onProgressUpdate(int percent) {
							progress.postValue(percent);
						}
					});
			if (httpResult.inputStream == null) {
				// Nothing to parse, the result is up-to-date.
				res = DownloadScheduleResult.upToDate();
				return;
			}

			try {
				Iterable<DetailedEvent> events = new EventsParser().parse(httpResult.inputStream);
				int count = scheduleDao.storeSchedule(events, httpResult.lastModified);
				res = DownloadScheduleResult.success(count);
			} finally {
				try {
					httpResult.inputStream.close();
				} catch (Exception ignored) {
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			res = DownloadScheduleResult.error();
		} finally {
			progress.postValue(100);
			result.postValue(new SingleEvent<>(res));
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

	public static LiveData<SingleEvent<DownloadScheduleResult>> getDownloadScheduleResult() {
		return result;
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
