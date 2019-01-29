package be.digitalia.fosdem.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import be.digitalia.fosdem.model.Track;

public class ThemeUtils {

	public static void setStatusBarTrackColor(@NonNull Activity activity, @NonNull Track.Type trackType) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final int color = ContextCompat.getColor(activity, trackType.getColorResId());
			final int darkColor = ContextCompat.getColor(activity, trackType.getDarkColorResId());
			activity.getWindow().setStatusBarColor(darkColor);
			final ActivityManager.TaskDescription taskDescription;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				taskDescription = new ActivityManager.TaskDescription(null, 0, color | 0xFF000000);
			} else {
				taskDescription = new ActivityManager.TaskDescription(null, null, color | 0xFF000000);
			}
			activity.setTaskDescription(taskDescription);
		}
	}
}
