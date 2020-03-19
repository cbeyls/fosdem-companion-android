package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.View
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
        emptyText = getString(R.string.no_search_result)
        isProgressBarVisible = true
        viewModel.results.observe(viewLifecycleOwner) { result ->
            adapter.submitList((result as? SearchViewModel.Result.Success)?.list)
            isProgressBarVisible = false
        }
    }

    companion object {
        fun newInstance() = SearchResultListFragment()
    }
}