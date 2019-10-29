package be.digitalia.fosdem.api;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.db.ScheduleDao;
import be.digitalia.fosdem.livedata.LiveDataFactory;
import be.digitalia.fosdem.livedata.SingleEvent;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.DetailedEvent;
import be.digitalia.fosdem.model.DownloadScheduleResult;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.parsers.EventsParser;
import be.digitalia.fosdem.utils.network.HttpUtils;
import okio.BufferedSource;

/**
 * Main API entry point.
 *
 * @author Christophe Beyls
 */
public class FosdemApi {

	// 8:30 (local time)
	private static final long DAY_START_TIME = 8 * DateUtils.HOUR_IN_MILLIS + 30 * DateUtils.MINUTE_IN_MILLIS;
	// 19:00 (local time)
	private static final long DAY_END_TIME = 19 * DateUtils.HOUR_IN_MILLIS;

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
	public static void downloadSchedule(@NonNull Context context) {
		if (!isLoading.compareAndSet(false, true)) {
			// If a download is already in progress, return immediately
			return;
		}
		final Context appContext = context.getApplicationContext();
		AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
			downloadScheduleInternal(appContext);
			isLoading.set(false);
		});
	}

	@WorkerThread
	private static void downloadScheduleInternal(@NonNull Context context) {
		progress.postValue(-1);
		DownloadScheduleResult res = DownloadScheduleResult.error();
		try {
			ScheduleDao scheduleDao = AppDatabase.getInstance(context).getScheduleDao();
			HttpUtils.Response httpResponse = HttpUtils.get(
					FosdemUrls.getSchedule(),
					scheduleDao.getLastModifiedTag(),
					progress::postValue);
			if (httpResponse.source == null) {
				// Nothing to parse, the result is up-to-date.
				res = DownloadScheduleResult.upToDate();
				return;
			}

			try (BufferedSource source = httpResponse.source) {
				Iterable<DetailedEvent> events = new EventsParser().parse(source);
				int count = scheduleDao.storeSchedule(events, httpResponse.lastModified);
				res = DownloadScheduleResult.success(count);
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
	public static LiveData<Map<String, RoomStatus>> getRoomStatuses(@NonNull Context context) {
		if (roomStatuses == null) {
			// The room statuses will only be loaded when the event is live.
			// Use the days from the database to determine it.
			final LiveData<List<Day>> daysLiveData = AppDatabase.getInstance(context).getScheduleDao().getDays();
			final LiveData<Boolean> scheduler = Transformations.switchMap(daysLiveData, days -> {
				final long[] startEndTimestamps = new long[days.size() * 2];
				int index = 0;
				for (Day day : days) {
					final long dayStart = day.getDate().getTime();
					startEndTimestamps[index++] = dayStart + DAY_START_TIME;
					startEndTimestamps[index++] = dayStart + DAY_END_TIME;
				}
				return LiveDataFactory.scheduler(startEndTimestamps);
			});
			final LiveData<Map<String, RoomStatus>> liveRoomStatuses = new LiveRoomStatusesLiveData();
			final LiveData<Map<String, RoomStatus>> offlineRoomStatuses = new MutableLiveData<>(Collections.emptyMap());
			roomStatuses = Transformations.switchMap(scheduler, isLive -> isLive ? liveRoomStatuses : offlineRoomStatuses);
			// Implementors: replace the above code with the next line to disable room status support
			// roomStatuses = new MutableLiveData<>();
		}
		return roomStatuses;
	}
}
