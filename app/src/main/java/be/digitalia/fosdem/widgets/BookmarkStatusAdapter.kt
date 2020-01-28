package be.digitalia.fosdem.widgets

import android.widget.ImageButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import be.digitalia.fosdem.R
import be.digitalia.fosdem.model.BookmarkStatus
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel

/**
 * Connect an ImageButton to a BookmarkStatusViewModel
 * to update its icon according to the current status and trigger a bookmark toggle on click.
 */
fun ImageButton.setupBookmarkStatus(viewModel: BookmarkStatusViewModel, owner: LifecycleOwner) {
    setOnClickListener { viewModel.toggleBookmarkStatus() }
    viewModel.bookmarkStatus.observe(owner) { bookmarkStatus: BookmarkStatus? ->
        if (bookmarkStatus == null) {
            isEnabled = false
            isSelected = false
        } else {
            val wasEnabled = isEnabled
            isEnabled = true
            contentDescription = context.getString(if (bookmarkStatus.isBookmarked) R.string.remove_bookmark else R.string.add_bookmark)
            isSelected = bookmarkStatus.isBookmarked
            // Only animate updates, when the button was already enabled
            if (!(bookmarkStatus.isUpdate && wasEnabled)) {
                jumpDrawablesToCurrentState()
            }
        }
    }
}