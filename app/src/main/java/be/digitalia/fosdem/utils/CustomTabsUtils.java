package be.digitalia.fosdem.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import be.digitalia.fosdem.R;

public class CustomTabsUtils {

	@NonNull
	@SuppressLint("PrivateResource")
	public static CustomTabsIntent.Builder configureToolbarColors(@NonNull CustomTabsIntent.Builder builder,
																  @NonNull Context context,
																  @ColorRes int toolbarColorResId) {
		final CustomTabColorSchemeParams darkColorSchemeParams = new CustomTabColorSchemeParams.Builder()
				.setToolbarColor(ContextCompat.getColor(context, R.color.design_dark_default_color_surface))
				.build();

		// Request the browser tab to follow the app theme setting
		final int colorScheme;
		switch (AppCompatDelegate.getDefaultNightMode()) {
			case AppCompatDelegate.MODE_NIGHT_NO:
				colorScheme = CustomTabsIntent.COLOR_SCHEME_LIGHT;
				break;
			case AppCompatDelegate.MODE_NIGHT_YES:
				colorScheme = CustomTabsIntent.COLOR_SCHEME_DARK;
				break;
			default:
				colorScheme = CustomTabsIntent.COLOR_SCHEME_SYSTEM;
		}

		return builder.setToolbarColor(ContextCompat.getColor(context, toolbarColorResId))
				.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkColorSchemeParams)
				.setColorScheme(colorScheme);
	}
}
