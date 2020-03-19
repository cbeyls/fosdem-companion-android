package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.viewmodels.LiveViewModel

sealed class LiveListFragment(@StringRes private val emptyTextResId: Int,
                              private val dataSourceProvider: (LiveViewModel) -> LiveData<PagedList<StatusEvent>>)
    : RecyclerViewFragment() {

    private val viewModel: LiveViewModel by viewModels({ requireParentFragment() })
    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        EventsAdapter(requireContext(), this, false)
    }

    private val preserveScrollPositionRunnable = Runnable {
        // Ensure we stay at scroll position 0 so we can see the insertion animation
        recyclerView?.run {
            if (scrollY == 0) {
                scrollToPosition(0)
            }
        }
    }

    override fun onRecyclerViewCreated(recyclerView: RecyclerView, savedInstanceState: Bundle?) = with(recyclerView) {
        val parent = parentFragment
        if (parent is RecycledViewPoolProvider) {
            setRecycledViewPool(parent.recycledViewPool)
        }

        layoutManager = LinearLayoutManager(context)
        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter(adapter)
        emptyText = getString(emptyTextResId)
        isProgressBarVisible = true

        dataSourceProvider(viewModel).observe(viewLifecycleOwner) { events ->
            adapter.submitList(events, preserveScrollPositionRunnable)
            isProgressBarVisible = false
        }
    }
}

class NextLiveListFragment : LiveListFragment(R.string.next_empty, LiveViewModel::nextEvents)
class NowLiveListFragment : LiveListFragment(R.string.now_empty, LiveViewModel::eventsInProgress)