package be.digitalia.fosdem.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import be.digitalia.fosdem.R;

public class ThemeUtils {

	@SuppressWarnings("deprecation")
	public static void setActivityColors(@NonNull Activity activity, @ColorInt int taskColor, @ColorInt int statusBarColor) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			activity.getWindow().setStatusBarColor(statusBarColor);
			final ActivityManager.TaskDescription taskDescription;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				taskDescription = new ActivityManager.TaskDescription(null, 0, taskColor | 0xFF000000);
			} else {
				taskDescription = new ActivityManager.TaskDescription(null, null, taskColor | 0xFF000000);
			}
			activity.setTaskDescription(taskDescription);
		}
	}

	public static void tintBackground(@NonNull View view, @Nullable ColorStateList backgroundColor) {
		final Drawable background = view.getBackground();
		if (background != null) {
			background.mutate();
			DrawableCompat.setTintList(background, backgroundColor);
		}
	}

	public static boolean isLightTheme(@NonNull Context context) {
		final TypedValue value = new TypedValue();
		return context.getTheme().resolveAttribute(R.attr.isLightTheme, value, true)
				&& value.data != 0;
	}

	public static void invertImageColors(@NonNull ImageView imageView) {
		final ColorFilter invertColorFilter = new ColorMatrixColorFilter(new float[]{
				-1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
				0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
				0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
				0.0f, 0.0f, 0.0f, 1.0f, 0.0f
		});
		imageView.setColorFilter(invertColorFilter);
	}
}
