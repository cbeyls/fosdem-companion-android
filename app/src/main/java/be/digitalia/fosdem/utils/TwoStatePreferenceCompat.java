package be.digitalia.fosdem.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.TwoStatePreference;

/**
 * Utility to retrieve the checked value from a two-state preference in a backwards-compatible way.
 */
public class TwoStatePreferenceCompat {

	private static final TwoStatePreferenceCompatImpl IMPL =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
					? new TwoStatePreferenceCompatICS() : new TwoStatePreferenceCompatBase();

	public static boolean isChecked(Preference preference) {
		return IMPL.isChecked(preference);
	}

	interface TwoStatePreferenceCompatImpl {
		boolean isChecked(Preference preference);
	}

	static class TwoStatePreferenceCompatBase implements TwoStatePreferenceCompatImpl {
		@Override
		public boolean isChecked(Preference preference) {
			return (preference instanceof CheckBoxPreference) && ((CheckBoxPreference) preference).isChecked();
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	static class TwoStatePreferenceCompatICS implements TwoStatePreferenceCompatImpl {
		@Override
		public boolean isChecked(Preference preference) {
			return (preference instanceof TwoStatePreference) && ((TwoStatePreference) preference).isChecked();
		}
	}
}
