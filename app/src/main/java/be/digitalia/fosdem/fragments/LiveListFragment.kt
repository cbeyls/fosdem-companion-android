package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.viewmodels.LiveViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
sealed class LiveListFragment(@StringRes private val emptyTextResId: Int,
                              private val dataSourceProvider: (LiveViewModel) -> LiveData<PagedList<StatusEvent>>)
    : Fragment(R.layout.recyclerview) {

    @Inject
    lateinit var api: FosdemApi
    private val viewModel: LiveViewModel by viewModels({ requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = EventsAdapter(view.context, false)
        api.roomStatuses.observe(viewLifecycleOwner, adapter)

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

        dataSourceProvider(viewModel).observe(viewLifecycleOwner) { events ->
            adapter.submitList(events) {
                // Ensure we stay at scroll position 0 so we can see the insertion animation
                holder.recyclerView.run {
                    if (scrollY == 0) {
                        scrollToPosition(0)
                    }
                }
            }
            holder.isProgressBarVisible = false
        }
    }
}

class NextLiveListFragment : LiveListFragment(R.string.next_empty, LiveViewModel::nextEvents)
class NowLiveListFragment : LiveListFragment(R.string.now_empty, LiveViewModel::eventsInProgress)