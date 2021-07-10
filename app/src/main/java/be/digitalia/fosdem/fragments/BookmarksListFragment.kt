package be.digitalia.fosdem.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NdefRecord
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.ExternalBookmarksActivity
import be.digitalia.fosdem.adapters.BookmarksAdapter
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.providers.BookmarksExportProvider
import be.digitalia.fosdem.utils.CreateNfcAppDataCallback
import be.digitalia.fosdem.utils.toBookmarksNfcAppData
import be.digitalia.fosdem.viewmodels.BookmarksViewModel
import be.digitalia.fosdem.widgets.MultiChoiceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.CancellationException
import javax.inject.Inject

/**
 * Bookmarks list, optionally filterable.
 *
 * @author Christophe Beyls
 */
@AndroidEntryPoint
class BookmarksListFragment : Fragment(R.layout.recyclerview), CreateNfcAppDataCallback {

    @Inject
    lateinit var api: FosdemApi
    private val viewModel: BookmarksViewModel by viewModels()
    private val multiChoiceHelper: MultiChoiceHelper by lazy(LazyThreadSafetyMode.NONE) {
        MultiChoiceHelper(requireActivity() as AppCompatActivity, this, object : MultiChoiceHelper.MultiChoiceModeListener {

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.action_mode_bookmarks, menu)
                return true
            }

            private fun updateSelectedCountDisplay(mode: ActionMode) {
                val count = multiChoiceHelper.checkedItemCount
                mode.title = resources.getQuantityString(R.plurals.selected, count, count)
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                updateSelectedCountDisplay(mode)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = when (item.itemId) {
                R.id.delete -> {
                    // Remove multiple bookmarks at once
                    viewModel.removeBookmarks(multiChoiceHelper.checkedItemIds)
                    mode.finish()
                    true
                }
                else -> false
            }

            override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
                updateSelectedCountDisplay(mode)
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
        })
    }
    private val getBookmarksLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            importBookmarks(uri)
        }
    }
    private var filterMenuItem: MenuItem? = null
    private var upcomingOnlyMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val upcomingOnly = requireActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(PREF_UPCOMING_ONLY, false)
        viewModel.upcomingOnly = upcomingOnly

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BookmarksAdapter(view.context, multiChoiceHelper)
        val holder = RecyclerViewViewHolder(view).apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(recyclerView.context)
                addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))
            }
            setAdapter(adapter)
            emptyText = getString(R.string.no_bookmark)
            isProgressBarVisible = true
        }

        api.roomStatuses.observe(viewLifecycleOwner) { statuses ->
            adapter.roomStatuses = statuses
        }
        viewModel.bookmarks.observe(viewLifecycleOwner) { bookmarks ->
            adapter.submitList(bookmarks)
            multiChoiceHelper.setAdapter(adapter, viewLifecycleOwner)
            holder.isProgressBarVisible = false
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
            val exportIntent = BookmarksExportProvider.getIntent(requireActivity())
            startActivity(Intent.createChooser(exportIntent, getString(R.string.export_bookmarks)))
            true
        }
        R.id.import_bookmarks -> {
            getBookmarksLauncher.launch(BookmarksExportProvider.TYPE)
            true
        }
        else -> false
    }

    private fun importBookmarks(uri: Uri) {
        lifecycleScope.launchWhenStarted {
            try {
                val bookmarkIds = viewModel.readBookmarkIds(uri)
                val intent = Intent(requireContext(), ExternalBookmarksActivity::class.java)
                        .putExtra(ExternalBookmarksActivity.EXTRA_BOOKMARK_IDS, bookmarkIds)
                startActivity(intent)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                ImportBookmarksErrorDialogFragment().show(parentFragmentManager, "importBookmarksError")
            }
        }
    }

    class ImportBookmarksErrorDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.import_bookmarks)
                    .setMessage(R.string.import_bookmarks_error)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
        }
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