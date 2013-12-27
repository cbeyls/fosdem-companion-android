package be.digitalia.fosdem.api;

import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.parsers.EventsParser;
import be.digitalia.fosdem.utils.HttpUtils;

/**
 * Main API entry point.
 * 
 * @author Christophe Beyls
 * 
 */
public class FosdemApi {

	// Local broadcasts parameters
	public static final String ACTION_SCHEDULE_PROGRESS = "ACTION_SCHEDULE_PROGRESS";
	public static final String EXTRA_PROGRESS = "EXTRA_PROGRESS";

	public static final int RESULT_ERROR = -1;
	private static final int RESULT_IN_PROGRESS = -2;

	private static final Object scheduleLock = new Object();
	private static volatile int scheduleResult;

	/**
	 * Download & store the schedule to the database. Only one thread will perform the actual action, the other ones will wait for the active thread to complete
	 * before returning its result.
	 * 
	 * @return The number of events processed, or RESULT_ERROR in case of error.
	 */
	public static int downloadSchedule(Context context) {
		synchronized (scheduleLock) {
			if (scheduleResult == RESULT_IN_PROGRESS) {
				do {
					try {
						scheduleLock.wait();
					} catch (InterruptedException e) {
					}
				} while (scheduleResult == RESULT_IN_PROGRESS);
				// After waiting for completion, return the result stored by the other thread
				return scheduleResult;
			}

			scheduleResult = RESULT_IN_PROGRESS;
		}

		int result;
		try {
			InputStream is = HttpUtils.get(context, FosdemUrls.getSchedule(), ACTION_SCHEDULE_PROGRESS, EXTRA_PROGRESS);
			try {
				Iterable<Event> events = new EventsParser().parse(is);
				result = DatabaseManager.getInstance().storeSchedule(events);
			} finally {
				try {
					is.close();
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Report completion in case of error to ensure a consistent UI
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_SCHEDULE_PROGRESS).putExtra(EXTRA_PROGRESS, 100));
			result = RESULT_ERROR;
		}

		synchronized (scheduleLock) {
			scheduleResult = result;
			scheduleLock.notifyAll();
		}
		return result;
	}
}
