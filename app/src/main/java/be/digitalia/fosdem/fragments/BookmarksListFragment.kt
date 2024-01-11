package be.digitalia.fosdem.fragments

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.ExternalBookmarksActivity
import be.digitalia.fosdem.adapters.BookmarksAdapter
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.providers.BookmarksExportProvider
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.viewmodels.BookmarksViewModel
import be.digitalia.fosdem.widgets.MultiChoiceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Named

/**
 * Bookmarks list, optionally filterable.
 *
 * @author Christophe Beyls
 */
@AndroidEntryPoint
class BookmarksListFragment : Fragment(R.layout.recyclerview) {

    @Inject
    lateinit var userSettingsProvider: UserSettingsProvider
    @Inject
    @Named("UIState")
    lateinit var preferences: SharedPreferences
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.hidePastEvents = preferences.getBoolean(HIDE_PAST_EVENTS_PREF_KEY, false)
    }

    private inner class BookmarksMenuProvider : MenuProvider {
        private var filterMenuItem: MenuItem? = null
        private var hidePastEventsMenuItem: MenuItem? = null

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.bookmarks, menu)
            filterMenuItem = menu.findItem(R.id.filter)
            hidePastEventsMenuItem = menu.findItem(R.id.hide_past_events)
            updateMenuItems()
        }

        override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
            R.id.hide_past_events -> {
                val hidePastEvents = !viewModel.hidePastEvents
                viewModel.hidePastEvents = hidePastEvents
                updateMenuItems()
                preferences.edit {
                    putBoolean(HIDE_PAST_EVENTS_PREF_KEY, hidePastEvents)
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

        private fun updateMenuItems() {
            val hidePastEvents = viewModel.hidePastEvents
            filterMenuItem?.setIcon(if (hidePastEvents) R.drawable.ic_filter_list_selected_white_24dp else R.drawable.ic_filter_list_white_24dp)
            hidePastEventsMenuItem?.isChecked = hidePastEvents
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(BookmarksMenuProvider(), viewLifecycleOwner)

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

        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            launch {
                userSettingsProvider.zoneId.collect { zoneId ->
                    adapter.zoneId = zoneId
                }
            }
            launch {
                api.roomStatuses.collect { statuses ->
                    adapter.roomStatuses = statuses
                }
            }
            launch {
                viewModel.bookmarks.filterNotNull().collect { bookmarks ->
                    adapter.submitList(bookmarks)
                    multiChoiceHelper.setAdapter(adapter, viewLifecycleOwner)
                    holder.isProgressBarVisible = false
                }
            }
        }
    }

    private fun importBookmarks(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bookmarkIds = viewModel.readBookmarkIds(uri)
                withStarted {
                    val intent = Intent(requireContext(), ExternalBookmarksActivity::class.java)
                        .putExtra(ExternalBookmarksActivity.EXTRA_BOOKMARK_IDS, bookmarkIds)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                withStarted {
                    ImportBookmarksErrorDialogFragment().show(
                        parentFragmentManager,
                        "importBookmarksError"
                    )
                }
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

    companion object {
        private const val HIDE_PAST_EVENTS_PREF_KEY = "bookmarks_upcoming_only"
    }
}