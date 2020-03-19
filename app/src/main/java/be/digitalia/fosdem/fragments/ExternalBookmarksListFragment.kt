package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.viewmodels.ExternalBookmarksViewModel

class ExternalBookmarksListFragment : RecyclerViewFragment() {

    private val viewModel: ExternalBookmarksViewModel by viewModels()
    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        EventsAdapter(requireContext(), this)
    }

    override fun onRecyclerViewCreated(recyclerView: RecyclerView, savedInstanceState: Bundle?) = with(recyclerView) {
        layoutManager = LinearLayoutManager(context)
        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter(adapter)
        emptyText = getString(R.string.no_bookmark)
        isProgressBarVisible = true

        val bookmarkIds = requireArguments().getLongArray(ARG_BOOKMARK_IDS)!!

        with(viewModel) {
            setBookmarkIds(bookmarkIds)
            bookmarks.observe(viewLifecycleOwner) { bookmarks ->
                adapter.submitList(bookmarks)
                isProgressBarVisible = false
            }
        }
    }

    companion object {
        private const val ARG_BOOKMARK_IDS = "bookmark_ids"

        fun newInstance(bookmarkIds: LongArray) = ExternalBookmarksListFragment().apply {
            arguments = Bundle(1).apply {
                putLongArray(ARG_BOOKMARK_IDS, bookmarkIds)
            }
        }
    }
}