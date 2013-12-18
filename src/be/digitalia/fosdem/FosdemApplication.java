package be.digitalia.fosdem;

import android.app.Application;
import be.digitalia.fosdem.db.DatabaseManager;

public class FosdemApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		DatabaseManager.init(this);
	}
}
