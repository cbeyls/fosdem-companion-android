package be.digitalia.fosdem.fragments

import android.content.Context
import android.content.Intent
import android.nfc.NdefRecord
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.BookmarksAdapter
import be.digitalia.fosdem.providers.BookmarksExportProvider
import be.digitalia.fosdem.utils.CreateNfcAppDataCallback
import be.digitalia.fosdem.utils.toBookmarksNfcAppData
import be.digitalia.fosdem.viewmodels.BookmarksViewModel
import be.digitalia.fosdem.widgets.MultiChoiceHelper

/**
 * Bookmarks list, optionally filterable.
 *
 * @author Christophe Beyls
 */
class BookmarksListFragment : RecyclerViewFragment(), CreateNfcAppDataCallback {

    private val viewModel: BookmarksViewModel by viewModels()
    private val adapter: BookmarksAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val multiChoiceModeListener = object : MultiChoiceHelper.MultiChoiceModeListener {

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.action_mode_bookmarks, menu)
                return true
            }

            private fun updateSelectedCountDisplay(mode: ActionMode) {
                val count = adapter.multiChoiceHelper.checkedItemCount
                mode.title = resources.getQuantityString(R.plurals.selected, count, count)
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                updateSelectedCountDisplay(mode)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = when (item.itemId) {
                R.id.delete -> {
                    // Remove multiple bookmarks at once
                    viewModel.removeBookmarks(adapter.multiChoiceHelper.checkedItemIds)
                    mode.finish()
                    true
                }
                else -> false
            }

            override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
                updateSelectedCountDisplay(mode)
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
        }

        BookmarksAdapter((requireActivity() as AppCompatActivity), this, multiChoiceModeListener)
    }
    private var filterMenuItem: MenuItem? = null
    private var upcomingOnlyMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val upcomingOnly = requireActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_UPCOMING_ONLY, false)
        viewModel.upcomingOnly = upcomingOnly

        setHasOptionsMenu(true)
    }

    override fun onRecyclerViewCreated(recyclerView: RecyclerView, savedInstanceState: Bundle?) = with(recyclerView) {
        layoutManager = LinearLayoutManager(recyclerView.context)
        addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter(adapter)
        emptyText = getString(R.string.no_bookmark)
        isProgressBarVisible = true

        viewModel.bookmarks.observe(viewLifecycleOwner) { bookmarks ->
            adapter.submitList(bookmarks)
            isProgressBarVisible = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bookmarks, menu)
        filterMenuItem = menu.findItem(R.id.filter)
        upcomingOnlyMenuItem = menu.findItem(R.id.upcoming_only)
        updateMenuItems()
    }

    private fun updateMenuItems() {
        val upcomingOnly = viewModel.upcomingOnly
        filterMenuItem?.setIcon(if (upcomingOnly) R.drawable.ic_filter_list_selected_white_24dp else R.drawable.ic_filter_list_white_24dp)
        upcomingOnlyMenuItem?.isChecked = upcomingOnly
    }

    override fun onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu()
        filterMenuItem = null
        upcomingOnlyMenuItem = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.upcoming_only -> {
            val upcomingOnly = !viewModel.upcomingOnly
            viewModel.upcomingOnly = upcomingOnly
            updateMenuItems()
            requireActivity().getPreferences(Context.MODE_PRIVATE).edit {
                putBoolean(PREF_UPCOMING_ONLY, upcomingOnly)
            }
            true
        }
        R.id.export_bookmarks -> {
            val exportIntent = BookmarksExportProvider.getIntent(activity)
            startActivity(Intent.createChooser(exportIntent, getString(R.string.export_bookmarks)))
            true
        }
        else -> false
    }

    override fun createNfcAppData(): NdefRecord? {
        val context = context ?: return null
        val bookmarks = viewModel.bookmarks.value
        return if (bookmarks.isNullOrEmpty()) {
            null
        } else bookmarks.toBookmarksNfcAppData(context)
    }

    companion object {
        private const val PREF_UPCOMING_ONLY = "bookmarks_upcoming_only"
    }
}