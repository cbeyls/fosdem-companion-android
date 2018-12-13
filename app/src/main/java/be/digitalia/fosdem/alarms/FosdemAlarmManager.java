package be.digitalia.fosdem.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.fragments.SettingsFragment;
import be.digitalia.fosdem.services.AlarmIntentService;

/**
 * This class monitors bookmarks and preferences changes to dispatch alarm update work to AlarmIntentService.
 *
 * @author Christophe Beyls
 */
public class FosdemAlarmManager implements OnSharedPreferenceChangeListener {

	private static FosdemAlarmManager instance;

	private final Context context;
	private boolean isEnabled;

	private final BroadcastReceiver scheduleRefreshedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// When the schedule DB is updated, update the alarms too
			startUpdateAlarms();
		}
	};

	private final BroadcastReceiver bookmarksReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Dispatch the Bookmark broadcasts to the service
			Intent serviceIntent = new Intent(intent);
			AlarmIntentService.enqueueWork(context, serviceIntent);
		}
	};

	public static void init(Context context) {
		if (instance == null) {
			instance = new FosdemAlarmManager(context);
		}
	}

	public static FosdemAlarmManager getInstance() {
		return instance;
	}

	private FosdemAlarmManager(Context context) {
		this.context = context;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		isEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED, false);
		if (isEnabled) {
			registerReceivers();
		}
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED.equals(key)) {
			isEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_PREF_NOTIFICATIONS_ENABLED, false);
			if (isEnabled) {
				registerReceivers();
				startUpdateAlarms();
			} else {
				unregisterReceivers();
				startDisableAlarms();
			}
		} else if (SettingsFragment.KEY_PREF_NOTIFICATIONS_DELAY.equals(key)) {
			startUpdateAlarms();
		}
	}

	private void registerReceivers() {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
		lbm.registerReceiver(scheduleRefreshedReceiver, new IntentFilter(DatabaseManager.ACTION_SCHEDULE_REFRESHED));
		IntentFilter filter = new IntentFilter();
		filter.addAction(DatabaseManager.ACTION_ADD_BOOKMARK);
		filter.addAction(DatabaseManager.ACTION_REMOVE_BOOKMARKS);
		lbm.registerReceiver(bookmarksReceiver, filter);
	}

	private void unregisterReceivers() {
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
		lbm.unregisterReceiver(scheduleRefreshedReceiver);
		lbm.unregisterReceiver(bookmarksReceiver);
	}

	void startUpdateAlarms() {
		Intent serviceIntent = new Intent(AlarmIntentService.ACTION_UPDATE_ALARMS);
		AlarmIntentService.enqueueWork(context, serviceIntent);
	}

	private void startDisableAlarms() {
		Intent serviceIntent = new Intent(AlarmIntentService.ACTION_DISABLE_ALARMS);
		AlarmIntentService.enqueueWork(context, serviceIntent);
	}
}
