package be.digitalia.fosdem.fragments;

import android.os.Bundle;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.viewmodels.SearchViewModel;

public class SearchResultListFragment extends RecyclerViewFragment implements Observer<SearchViewModel.Result> {

	private EventsAdapter adapter;

	public static SearchResultListFragment newInstance() {
		return new SearchResultListFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new EventsAdapter(requireContext(), this);
	}

	@Override
	protected void onRecyclerViewCreated(RecyclerView recyclerView, Bundle savedInstanceState) {
		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
		recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setAdapter(adapter);
		setEmptyText(getString(R.string.no_search_result));
		setProgressBarVisible(true);

		final SearchViewModel viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
		viewModel.getResults().observe(getViewLifecycleOwner(), this);
	}

	@Override
	public void onChanged(SearchViewModel.Result result) {
		if (result instanceof SearchViewModel.Result.Success) {
			adapter.submitList(((SearchViewModel.Result.Success) result).getList());
		} else {
			adapter.submitList(null);
		}
		setProgressBarVisible(false);
	}
}
