package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.viewmodels.SearchViewModel

class SearchResultListFragment : Fragment(R.layout.recyclerview) {

    private val viewModel: SearchViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = EventsAdapter(view.context, viewLifecycleOwner)
        val holder = RecyclerViewViewHolder(view).apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
            setAdapter(adapter)
            emptyText = getString(R.string.no_search_result)
            isProgressBarVisible = true
        }

        viewModel.results.observe(viewLifecycleOwner) { result ->
            adapter.submitList((result as? SearchViewModel.Result.Success)?.list)
            holder.isProgressBarVisible = false
        }
    }

    companion object {
        fun newInstance() = SearchResultListFragment()
    }
}