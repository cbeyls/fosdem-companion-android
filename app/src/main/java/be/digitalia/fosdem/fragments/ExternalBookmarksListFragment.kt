package be.digitalia.fosdem.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.viewmodels.ExternalBookmarksViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExternalBookmarksListFragment : Fragment(R.layout.recyclerview) {

    @Inject
    lateinit var api: FosdemApi
    private val viewModel: ExternalBookmarksViewModel by viewModels()
    private var addAllMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setFragmentResultListener(REQUEST_KEY_CONFIRM_ADD_ALL) { _, _ -> viewModel.addAll() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = EventsAdapter(view.context)
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

        api.roomStatuses.observe(viewLifecycleOwner) { statuses ->
            adapter.roomStatuses = statuses
        }
        with(viewModel) {
            setBookmarkIds(bookmarkIds)
            bookmarks.observe(viewLifecycleOwner) { bookmarks ->
                adapter.submitList(bookmarks)
                addAllMenuItem?.isEnabled = bookmarks.isNotEmpty()
                holder.isProgressBarVisible = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.external_bookmarks, menu)
        menu.findItem(R.id.add_all)?.let { item ->
            val bookmarks = viewModel.bookmarks.value
            item.isEnabled = bookmarks != null && bookmarks.isNotEmpty()
            addAllMenuItem = item
        }
    }

    override fun onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu()
        addAllMenuItem = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_all -> {
            ConfirmAddAllDialogFragment().show(parentFragmentManager, "confirmAddAll")
            true
        }
        else -> false
    }

    class ConfirmAddAllDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.external_bookmarks_add_all_confirmation_title)
                    .setMessage(R.string.external_bookmarks_add_all_confirmation_text)
                    .setPositiveButton(android.R.string.ok) { _, _ -> setFragmentResult(REQUEST_KEY_CONFIRM_ADD_ALL, Bundle()) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
        }
    }

    companion object {
        private const val ARG_BOOKMARK_IDS = "bookmark_ids"
        private const val REQUEST_KEY_CONFIRM_ADD_ALL = "confirm_add_all"

        fun createArguments(bookmarkIds: LongArray) = Bundle(1).apply {
            putLongArray(ARG_BOOKMARK_IDS, bookmarkIds)
        }
    }
}