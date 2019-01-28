package be.digitalia.fosdem;

import android.app.Application;
import androidx.preference.PreferenceManager;
import be.digitalia.fosdem.alarms.FosdemAlarmManager;

public class FosdemApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Initialize settings
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		// Alarms (requires settings)
		FosdemAlarmManager.init(this);
	}
}
