package be.digitalia.fosdem.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import be.digitalia.fosdem.R;

public class MaterialHorizontalProgressBar extends ProgressBar {

	public MaterialHorizontalProgressBar(Context context) {
		super(context, null, R.attr.materialHorizontalProgressBarStyle);
		init();
	}

	public MaterialHorizontalProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs, R.attr.materialHorizontalProgressBarStyle);
		init();
	}

	public MaterialHorizontalProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			setProgressDrawable(createProgressDrawable());
			setIndeterminateDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.avd_progress_indeterminate_horizontal));
		}
	}

	private Drawable createProgressDrawable() {
		final LayerDrawable layerDrawable = (LayerDrawable) ContextCompat.getDrawable(getContext(), R.drawable.progress_horizontal_material);
		if (layerDrawable != null) {
			final TypedArray a = getContext().getTheme().obtainStyledAttributes(
					new int[]{R.attr.colorControlNormal, R.attr.colorControlActivated, android.R.attr.disabledAlpha}
			);
			final int colorControlNormal = a.getColor(0, Color.TRANSPARENT);
			final int colorControlActivated = a.getColor(1, Color.TRANSPARENT);
			final int disabledAlpha = Math.max(0, Math.min(255, Math.round(a.getFloat(2, 0f) * 255f)));
			a.recycle();

			final Drawable backgroundDrawable = layerDrawable.findDrawableByLayerId(android.R.id.background);
			backgroundDrawable.setAlpha(disabledAlpha);
			backgroundDrawable.mutate().setColorFilter(new PorterDuffColorFilter(colorControlNormal, PorterDuff.Mode.SRC_IN));

			final Drawable secondaryProgressDrawable = layerDrawable.findDrawableByLayerId(android.R.id.secondaryProgress);
			secondaryProgressDrawable.setAlpha(disabledAlpha);
			secondaryProgressDrawable.mutate().setColorFilter(new PorterDuffColorFilter(colorControlActivated, PorterDuff.Mode.SRC_IN));

			final Drawable progressDrawable = layerDrawable.findDrawableByLayerId(android.R.id.progress);
			progressDrawable.mutate().setColorFilter(new PorterDuffColorFilter(colorControlActivated, PorterDuff.Mode.SRC_IN));
		}
		return layerDrawable;
	}
}
