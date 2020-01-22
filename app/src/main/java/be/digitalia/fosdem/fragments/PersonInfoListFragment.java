package be.digitalia.fosdem.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.adapters.ConcatAdapter;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.utils.CustomTabsUtils;
import be.digitalia.fosdem.utils.DateUtils;
import be.digitalia.fosdem.viewmodels.PersonInfoViewModel;

public class PersonInfoListFragment extends RecyclerViewFragment implements Observer<PagedList<StatusEvent>> {

	private static final String ARG_PERSON = "person";

	private Person person;
	private EventsAdapter adapter;

	public static PersonInfoListFragment newInstance(Person person) {
		PersonInfoListFragment f = new PersonInfoListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_PERSON, person);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new EventsAdapter(getContext(), this);
		person = requireArguments().getParcelable(ARG_PERSON);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.person, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.more_info:
				// Look for the first non-placeholder event in the paged list
				final PagedList<StatusEvent> list = adapter.getCurrentList();
				final int size = (list == null) ? 0 : list.size();
				StatusEvent statusEvent = null;
				for (int i = 0; i < size; ++i) {
					statusEvent = list.get(i);
					if (statusEvent != null) {
						break;
					}
				}
				if (statusEvent != null) {
					final int year = DateUtils.getYear(statusEvent.getEvent().getDay().getDate().getTime());
					String url = person.getUrl(year);
					if (url != null) {
						try {
							final Context context = requireContext();
							CustomTabsUtils.configureToolbarColors(new CustomTabsIntent.Builder(), context, R.color.light_color_primary)
									.setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
									.setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
									.build()
									.launchUrl(context, Uri.parse(url));
						} catch (ActivityNotFoundException ignore) {
						}
					}
				}
				return true;
		}
		return false;
	}

	@Override
	protected void onRecyclerViewCreated(RecyclerView recyclerView, Bundle savedInstanceState) {
		final int contentMargin = getResources().getDimensionPixelSize(R.dimen.content_margin);
		recyclerView.setPadding(contentMargin, contentMargin, contentMargin, contentMargin);
		recyclerView.setClipToPadding(false);
		recyclerView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setAdapter(new ConcatAdapter(new HeaderAdapter(), adapter));
		setEmptyText(getString(R.string.no_data));
		setProgressBarVisible(true);

		final PersonInfoViewModel viewModel = new ViewModelProvider(this).get(PersonInfoViewModel.class);
		viewModel.setPerson(person);
		viewModel.getEvents().observe(getViewLifecycleOwner(), this);
	}

	@Override
	public void onChanged(PagedList<StatusEvent> events) {
		adapter.submitList(events);
		setProgressBarVisible(false);
	}

	static class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.ViewHolder> {

		@Override
		public int getItemCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			return R.layout.header_person_info;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_person_info, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			// Nothing to bind
		}

		static class ViewHolder extends RecyclerView.ViewHolder {

			ViewHolder(View itemView) {
				super(itemView);
			}
		}
	}
}
