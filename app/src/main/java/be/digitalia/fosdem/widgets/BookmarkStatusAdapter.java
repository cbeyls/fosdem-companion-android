package be.digitalia.fosdem.widgets;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel;

public class BookmarkStatusAdapter {

	private BookmarkStatusAdapter() {
	}

	/**
	 * Connect an ImageButton to a BookmarkStatusViewModel
	 * to update its icon according to the current status and trigger a bookmark toggle on click.
	 */
	public static void setupWithImageButton(@NonNull final BookmarkStatusViewModel viewModel, @NonNull LifecycleOwner owner,
											@NonNull final ImageButton imageButton) {
		imageButton.setOnClickListener(v -> viewModel.toggleBookmarkStatus());
		viewModel.getBookmarkStatus().observe(owner, bookmarkStatus -> {
			if (bookmarkStatus == null) {
				imageButton.setEnabled(false);
				imageButton.setImageResource(R.drawable.ic_bookmark_outline_white_24dp);
			} else {
				// Only animate updates, when the button was already enabled
				final boolean animate = bookmarkStatus.isUpdate() && imageButton.isEnabled();
				imageButton.setEnabled(true);
				if (bookmarkStatus.isBookmarked()) {
					imageButton.setContentDescription(imageButton.getContext().getString(R.string.remove_bookmark));
					imageButton.setImageResource(animate ? R.drawable.avd_bookmark_add_24dp : R.drawable.ic_bookmark_white_24dp);
				} else {
					imageButton.setContentDescription(imageButton.getContext().getString(R.string.add_bookmark));
					imageButton.setImageResource(animate ? R.drawable.avd_bookmark_remove_24dp : R.drawable.ic_bookmark_outline_white_24dp);
				}
				final Drawable drawable = imageButton.getDrawable();
				if (drawable instanceof Animatable) {
					((Animatable) drawable).start();
				}
			}
		});
	}
}
