package be.digitalia.fosdem.alarms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import androidx.preference.PreferenceManager;
import be.digitalia.fosdem.fragments.SettingsFragment;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.services.AlarmIntentService;

import java.util.Date;

/**
 * This class monitors bookmarks and preferences changes to dispatch alarm update work to AlarmIntentService.
 *
 * @author Christophe Beyls
 */
public class FosdemAlarmManager implements OnSharedPreferenceChangeListener {

	private static FosdemAlarmManager instance;

	private final Context context;
	private volatile boolean isEnabled;

	public static void init(Context context) {
		if (instance == null) {
			instance = new FosdemAlarmManager(context);
		}
	}

	public static FosdemAlarmManager getInstance() {
		return instance;
	}

	private FosdemAlarmManager(Context context) {
		this.context = context.getApplicationContext();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
		isEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED, false);
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void onScheduleRefreshed() {
		if (isEnabled) {
			startUpdateAlarms();
		}
	}

	public void onBookmarkAdded(Event event) {
		if (isEnabled) {
			Intent serviceIntent = new Intent(AlarmIntentService.ACTION_ADD_BOOKMARK)
					.putExtra(AlarmIntentService.EXTRA_EVENT_ID, event.getId());
			final Date startTime = event.getStartTime();
			if (startTime != null) {
				serviceIntent.putExtra(AlarmIntentService.EXTRA_EVENT_START_TIME, startTime.getTime());
			}
			AlarmIntentService.enqueueWork(context, serviceIntent);
		}
	}

	public void onBookmarksRemoved(long[] eventIds) {
		if (isEnabled) {
			Intent serviceIntent = new Intent(AlarmIntentService.ACTION_REMOVE_BOOKMARKS)
					.putExtra(AlarmIntentService.EXTRA_EVENT_IDS, eventIds);
			AlarmIntentService.enqueueWork(context, serviceIntent);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED.equals(key)) {
			final boolean isEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED, false);
			this.isEnabled = isEnabled;
			if (isEnabled) {
				startUpdateAlarms();
			} else {
				startDisableAlarms();
			}
		} else if (SettingsFragment.KEY_PREF_NOTIFICATIONS_DELAY.equals(key)) {
			startUpdateAlarms();
		}
	}

	private void startUpdateAlarms() {
		Intent serviceIntent = new Intent(AlarmIntentService.ACTION_UPDATE_ALARMS);
		AlarmIntentService.enqueueWork(context, serviceIntent);
	}

	private void startDisableAlarms() {
		Intent serviceIntent = new Intent(AlarmIntentService.ACTION_DISABLE_ALARMS);
		AlarmIntentService.enqueueWork(context, serviceIntent);
	}
}
