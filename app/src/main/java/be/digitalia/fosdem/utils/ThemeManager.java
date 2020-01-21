package be.digitalia.fosdem.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import be.digitalia.fosdem.fragments.SettingsFragment;

public class ThemeManager implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static ThemeManager instance;

	private ThemeManager(@NonNull Context context) {
		final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		updateTheme(defaultSharedPreferences);
		defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public static void init(Context context) {
		if (instance == null) {
			instance = new ThemeManager(context);
		}
	}

	public static ThemeManager getInstance() {
		return instance;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (SettingsFragment.KEY_PREF_THEME.equals(key)) {
			updateTheme(sharedPreferences);
		}
	}

	private void updateTheme(SharedPreferences sharedPreferences) {
		final String stringMode = sharedPreferences.getString(SettingsFragment.KEY_PREF_THEME, String.valueOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
		AppCompatDelegate.setDefaultNightMode(Integer.parseInt(stringMode));
	}
}
