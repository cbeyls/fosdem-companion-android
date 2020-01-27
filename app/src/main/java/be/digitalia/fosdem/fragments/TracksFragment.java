package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.utils.RecyclerViewUtils;

public class TracksFragment extends Fragment implements RecycledViewPoolProvider, Observer<List<Day>> {

	static class ViewHolder {
		View contentView;
		View emptyView;
		ViewPager2 pager;
		TabLayout tabs;
		DaysAdapter daysAdapter;
		RecyclerView.RecycledViewPool recycledViewPool;
	}

	private static final String PREF_CURRENT_PAGE = "tracks_current_page";

	private ViewHolder holder;
	private int savedCurrentPage = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			// Restore the current page from preferences
			savedCurrentPage = requireActivity().getPreferences(Context.MODE_PRIVATE).getInt(PREF_CURRENT_PAGE, -1);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_tracks, container, false);

		holder = new ViewHolder();
		holder.contentView = view.findViewById(R.id.content);
		holder.emptyView = view.findViewById(android.R.id.empty);
		holder.pager = view.findViewById(R.id.pager);
		holder.pager.setOffscreenPageLimit(1);
		RecyclerViewUtils.enforceSingleScrollDirection(RecyclerViewUtils.getRecyclerView(holder.pager));
		holder.tabs = view.findViewById(R.id.tabs);
		holder.daysAdapter = new DaysAdapter(this);
		holder.recycledViewPool = new RecyclerView.RecycledViewPool();

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		holder = null;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		AppDatabase.getInstance(requireContext()).getScheduleDao().getDays()
				.observe(getViewLifecycleOwner(), this);
	}

	@Override
	public void onStop() {
		super.onStop();
		// Save the current page to preferences if it has changed
		final int page = holder.pager.getCurrentItem();
		SharedPreferences prefs = requireActivity().getPreferences(Context.MODE_PRIVATE);
		if (prefs.getInt(PREF_CURRENT_PAGE, -1) != page) {
			prefs.edit()
					.putInt(PREF_CURRENT_PAGE, page)
					.apply();
		}
	}

	@Override
	public RecyclerView.RecycledViewPool getRecycledViewPool() {
		return (holder == null) ? null : holder.recycledViewPool;
	}

	@Override
	public void onChanged(@Nullable List<Day> days) {
		holder.daysAdapter.setDays(days);

		final int totalPages = holder.daysAdapter.getItemCount();
		if (totalPages == 0) {
			holder.contentView.setVisibility(View.GONE);
			holder.emptyView.setVisibility(View.VISIBLE);
		} else {
			holder.contentView.setVisibility(View.VISIBLE);
			holder.emptyView.setVisibility(View.GONE);
			if (holder.pager.getAdapter() == null) {
				holder.pager.setAdapter(holder.daysAdapter);
				new TabLayoutMediator(holder.tabs, holder.pager,
						(tab, position) -> tab.setText(holder.daysAdapter.getPageTitle(position))).attach();
			}
			if (savedCurrentPage != -1) {
				holder.pager.setCurrentItem(Math.min(savedCurrentPage, totalPages - 1), false);
				savedCurrentPage = -1;
			}
		}
	}

	private static class DaysAdapter extends FragmentStateAdapter {

		private List<Day> days;

		DaysAdapter(Fragment fragment) {
			super(fragment.getChildFragmentManager(), fragment.getViewLifecycleOwner().getLifecycle());
		}

		void setDays(List<Day> days) {
			if (this.days != days) {
				this.days = days;
				notifyDataSetChanged();
			}
		}

		@Override
		public int getItemCount() {
			return (days == null) ? 0 : days.size();
		}

		@Override
		public long getItemId(int position) {
			return days.get(position).getIndex();
		}

		@Override
		public boolean containsItem(long itemId) {
			final int count = getItemCount();
			for (int i = 0; i < count; ++i) {
				if (days.get(i).getIndex() == itemId) {
					return true;
				}
			}
			return false;
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			return TracksListFragment.newInstance(days.get(position));
		}

		CharSequence getPageTitle(int position) {
			return days.get(position).toString();
		}
	}
}
