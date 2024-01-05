package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.viewmodels.LiveViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
sealed class LiveListFragment(
    @StringRes private val emptyTextResId: Int,
    private val dataSourceProvider: (LiveViewModel) -> Flow<PagingData<StatusEvent>>
) : Fragment(R.layout.recyclerview) {

    @Inject
    lateinit var userSettingsProvider: UserSettingsProvider
    @Inject
    lateinit var api: FosdemApi
    private val viewModel: LiveViewModel by viewModels({ requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = EventsAdapter(view.context, false)
        val holder = RecyclerViewViewHolder(view).apply {
            recyclerView.apply {
                val parent = parentFragment
                if (parent is RecycledViewPoolProvider) {
                    setRecycledViewPool(parent.recycledViewPool)
                }

                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
            setAdapter(adapter)
            emptyText = getString(emptyTextResId)
            isProgressBarVisible = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow
                .distinctUntilChangedBy { it.refresh }
                .filter { it.refresh !is LoadState.Loading }
                .collect {
                    holder.isProgressBarVisible = false
                    // Ensure we stay at scroll position 0 so we can see the insertion animation
                    with(holder.recyclerView) {
                        if ((layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() == 0) {
                            scrollToPosition(0)
                        }
                    }
                }
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
                dataSourceProvider(viewModel).collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }
    }
}

class NextLiveListFragment : LiveListFragment(R.string.next_empty, LiveViewModel::nextEvents)
class NowLiveListFragment : LiveListFragment(R.string.now_empty, LiveViewModel::eventsInProgress)