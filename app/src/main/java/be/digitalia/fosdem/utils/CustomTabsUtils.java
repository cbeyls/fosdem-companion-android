package be.digitalia.fosdem.utils;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

public class CustomTabsUtils {

	@NonNull
	public static CustomTabsIntent.Builder configureToolbarColors(@NonNull CustomTabsIntent.Builder builder,
																  @NonNull Context context,
																  @ColorRes int lightColorResId) {
		final CustomTabColorSchemeParams darkColorSchemeParams = new CustomTabColorSchemeParams.Builder()
				.setToolbarColor(ThemeUtils.getColorSurface(context))
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

		// Use the application Context so that the default (non-night) color value is used
		return builder.setToolbarColor(ContextCompat.getColor(context.getApplicationContext(), lightColorResId))
				.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkColorSchemeParams)
				.setColorScheme(colorScheme);
	}
}
