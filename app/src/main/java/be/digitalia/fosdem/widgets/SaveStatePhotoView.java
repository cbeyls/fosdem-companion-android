package be.digitalia.fosdem.widgets;

import android.content.Context;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import com.github.chrisbanes.photoview.PhotoView;

/**
 * PhotoView which saves and restores the current scale and approximate position.
 */
public class SaveStatePhotoView extends PhotoView {

	public SaveStatePhotoView(Context context) {
		super(context);
	}

	public SaveStatePhotoView(Context context, AttributeSet attr) {
		super(context, attr);
	}

	public SaveStatePhotoView(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
		ss.scale = getScale();
		final RectF rect = getDisplayRect();
		final float overflowWidth = rect.width() - getWidth();
		if (overflowWidth > 0f) {
			ss.pivotX = -rect.left / overflowWidth;
		}
		final float overflowHeight = rect.height() - getHeight();
		if (overflowHeight > 0f) {
			ss.pivotY = -rect.top / overflowHeight;
		}
		return ss;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		final SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				float scale = Math.max(ss.scale, getMinimumScale());
				scale = Math.min(scale, getMaximumScale());
				setScale(scale, getWidth() * ss.pivotX, getHeight() * ss.pivotY, false);
				getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});
	}

	public static class SavedState extends BaseSavedState {
		float scale = 1f;
		float pivotX = 0.5f;
		float pivotY = 0.5f;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeFloat(scale);
			out.writeFloat(pivotX);
			out.writeFloat(pivotY);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};

		SavedState(Parcel in) {
			super(in);
			scale = in.readFloat();
			pivotX = in.readFloat();
			pivotY = in.readFloat();
		}
	}
}
