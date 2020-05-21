package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.viewmodels.ExternalBookmarksViewModel

class ExternalBookmarksListFragment : Fragment(R.layout.recyclerview) {

    private val viewModel: ExternalBookmarksViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = EventsAdapter(view.context, viewLifecycleOwner)
        val holder = RecyclerViewViewHolder(view).apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
            setAdapter(adapter)
            emptyText = getString(R.string.no_bookmark)
            isProgressBarVisible = true
        }

        val bookmarkIds = requireArguments().getLongArray(ARG_BOOKMARK_IDS)!!

        with(viewModel) {
            setBookmarkIds(bookmarkIds)
            bookmarks.observe(viewLifecycleOwner) { bookmarks ->
                adapter.submitList(bookmarks)
                holder.isProgressBarVisible = false
            }
        }
    }

    companion object {
        private const val ARG_BOOKMARK_IDS = "bookmark_ids"

        fun createArguments(bookmarkIds: LongArray) = Bundle(1).apply {
            putLongArray(ARG_BOOKMARK_IDS, bookmarkIds)
        }
    }
}