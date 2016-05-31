package be.digitalia.fosdem.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import be.digitalia.fosdem.widgets.SlidingTabLayout;

import java.util.List;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.GlobalCacheLoader;
import be.digitalia.fosdem.model.Day;

public class TracksFragment extends Fragment implements LoaderCallbacks<List<Day>> {

	private static class ViewHolder {
		View contentView;
		View emptyView;
		ViewPager pager;
		SlidingTabLayout slidingTabs;
		DaysAdapter daysAdapter;
	}

	private static final int DAYS_LOADER_ID = 1;
	private static final String PREF_CURRENT_PAGE = "tracks_current_page";

	private ViewHolder holder;
	private int savedCurrentPage = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			// Restore the current page from preferences
			savedCurrentPage = getActivity().getPreferences(Context.MODE_PRIVATE).getInt(PREF_CURRENT_PAGE, -1);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_tracks, container, false);

		holder = new ViewHolder();
		holder.contentView = view.findViewById(R.id.content);
		holder.emptyView = view.findViewById(android.R.id.empty);
		holder.pager = (ViewPager) view.findViewById(R.id.pager);
		holder.slidingTabs = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
		holder.daysAdapter = new DaysAdapter(getChildFragmentManager());

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

		getLoaderManager().initLoader(DAYS_LOADER_ID, null, this);
	}

	@Override
	public void onStop() {
		super.onStop();
		// Save the current page to preferences if it has changed
		final int page = holder.pager.getCurrentItem();
		SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		if (prefs.getInt(PREF_CURRENT_PAGE, -1) != page) {
			prefs.edit().putInt(PREF_CURRENT_PAGE, page).commit();
		}
	}

	private static class DaysLoader extends GlobalCacheLoader<List<Day>> {

		private final BroadcastReceiver scheduleRefreshedReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				onContentChanged();
			}
		};

		public DaysLoader(Context context) {
			super(context);
			// Reload days list when the schedule has been refreshed
			LocalBroadcastManager.getInstance(context).registerReceiver(scheduleRefreshedReceiver,
					new IntentFilter(DatabaseManager.ACTION_SCHEDULE_REFRESHED));
		}

		@Override
		protected void onReset() {
			super.onReset();
			LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(scheduleRefreshedReceiver);
		}

		@Override
		protected List<Day> getCachedResult() {
			return DatabaseManager.getInstance().getCachedDays();
		}

		@Override
		public List<Day> loadInBackground() {
			return DatabaseManager.getInstance().getDays();
		}
	}

	@Override
	public Loader<List<Day>> onCreateLoader(int id, Bundle args) {
		return new DaysLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<Day>> loader, List<Day> data) {
		holder.daysAdapter.setDays(data);

		final int totalPages = holder.daysAdapter.getCount();
		if (totalPages == 0) {
			holder.contentView.setVisibility(View.GONE);
			holder.emptyView.setVisibility(View.VISIBLE);
		} else {
			holder.contentView.setVisibility(View.VISIBLE);
			holder.emptyView.setVisibility(View.GONE);
			if (holder.pager.getAdapter() == null) {
				holder.pager.setAdapter(holder.daysAdapter);
				holder.slidingTabs.setViewPager(holder.pager);
			}
			if (savedCurrentPage != -1) {
				holder.pager.setCurrentItem(Math.min(savedCurrentPage, totalPages - 1), false);
				savedCurrentPage = -1;
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<List<Day>> loader) {
	}

	private static class DaysAdapter extends FragmentStatePagerAdapter {

		private List<Day> days;

		public DaysAdapter(FragmentManager fm) {
			super(fm);
		}

		public void setDays(List<Day> days) {
			if (this.days != days) {
				this.days = days;
				notifyDataSetChanged();
			}
		}

		@Override
		public int getCount() {
			return (days == null) ? 0 : days.size();
		}

		@Override
		public Fragment getItem(int position) {
			return TracksListFragment.newInstance(days.get(position));
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return days.get(position).toString();
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			// Allow the non-primary fragments to start as soon as they are visible
			Fragment f = (Fragment) super.instantiateItem(container, position);
			f.setUserVisibleHint(true);
			return f;
		}
	}
}
