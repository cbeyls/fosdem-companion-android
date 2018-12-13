package be.digitalia.fosdem.fragments;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;
import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.services.AlarmIntentService;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String KEY_PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
	// Android >= O only
	public static final String KEY_PREF_NOTIFICATIONS_CHANNEL = "notifications_channel";
	// Android < O only
	public static final String KEY_PREF_NOTIFICATIONS_VIBRATE = "notifications_vibrate";
	// Android < O only
	public static final String KEY_PREF_NOTIFICATIONS_LED = "notifications_led";
	public static final String KEY_PREF_NOTIFICATIONS_DELAY = "notifications_delay";
	private static final String KEY_PREF_ABOUT = "about";
	private static final String KEY_PREF_VERSION = "version";

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.settings, rootKey);
		updateNotificationsEnabled();
		updateNotificationsDelaySummary();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			setupNotificationsChannel();
		}
		setupAboutDialog();
		populateVersion();
	}

	@Override
	public void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (KEY_PREF_NOTIFICATIONS_ENABLED.equals(key)) {
			updateNotificationsEnabled();
		} else if (KEY_PREF_NOTIFICATIONS_DELAY.equals(key)) {
			updateNotificationsDelaySummary();
		}
	}

	private void updateNotificationsEnabled() {
		boolean notificationsEnabled = ((TwoStatePreference) findPreference(KEY_PREF_NOTIFICATIONS_ENABLED)).isChecked();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			findPreference(KEY_PREF_NOTIFICATIONS_CHANNEL).setEnabled(notificationsEnabled);
		} else {
			findPreference(KEY_PREF_NOTIFICATIONS_VIBRATE).setEnabled(notificationsEnabled);
			findPreference(KEY_PREF_NOTIFICATIONS_LED).setEnabled(notificationsEnabled);
		}
		findPreference(KEY_PREF_NOTIFICATIONS_DELAY).setEnabled(notificationsEnabled);
	}

	private void updateNotificationsDelaySummary() {
		ListPreference notificationsDelayPreference = (ListPreference) findPreference(KEY_PREF_NOTIFICATIONS_DELAY);
		notificationsDelayPreference.setSummary(notificationsDelayPreference.getEntry());
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void setupNotificationsChannel() {
		findPreference(KEY_PREF_NOTIFICATIONS_CHANNEL).setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						AlarmIntentService.startChannelNotificationSettingsActivity(getContext());
						return true;
					}
				});
	}

	private void setupAboutDialog() {
		findPreference(KEY_PREF_ABOUT).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new AboutDialogFragment().show(getFragmentManager(), "about");
				return true;
			}
		});
	}

	public static class AboutDialogFragment extends DialogFragment {

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getContext())
					.setTitle(R.string.app_name)
					.setIcon(R.mipmap.ic_launcher)
					.setMessage(getResources().getText(R.string.about_text))
					.setPositiveButton(android.R.string.ok, null)
					.create();
		}

		@Override
		public void onStart() {
			super.onStart();
			// Make links clickable; must be called after the dialog is shown
			((TextView) getDialog().findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	private void populateVersion() {
		findPreference(KEY_PREF_VERSION).setSummary(BuildConfig.VERSION_NAME);
	}
}
