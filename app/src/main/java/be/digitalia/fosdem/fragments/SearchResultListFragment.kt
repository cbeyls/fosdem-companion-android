package be.digitalia.fosdem.fragments

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.viewmodels.SearchViewModel

class SearchResultListFragment : RecyclerViewFragment() {

    private val viewModel: SearchViewModel by activityViewModels()
    private val adapter: EventsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        EventsAdapter(requireContext(), this)
    }

    override fun onRecyclerViewCreated(recyclerView: RecyclerView, savedInstanceState: Bundle?) {
        with(recyclerView) {
            layoutManager = LinearLayoutManager(recyclerView.context)
            addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setAdapter(adapter)
        setEmptyText(getString(R.string.no_search_result))
        setProgressBarVisible(true)
        viewModel.results.observe(viewLifecycleOwner) { result ->
            adapter.submitList((result as? SearchViewModel.Result.Success)?.list)
            setProgressBarVisible(false)
        }
    }

    companion object {
        fun newInstance() = SearchResultListFragment()
    }
}