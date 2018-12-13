package be.digitalia.fosdem;

import android.app.Application;

import androidx.preference.PreferenceManager;
import be.digitalia.fosdem.alarms.FosdemAlarmManager;
import be.digitalia.fosdem.db.DatabaseManager;

public class FosdemApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		DatabaseManager.init(this);
		// Initialize settings
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		// Alarms (requires settings)
		FosdemAlarmManager.init(this);
	}
}
