package be.digitalia.fosdem.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.widgets.SlidingTabLayout;

public class LiveFragment extends Fragment implements RecycledViewPoolProvider {

	static class ViewHolder {
		ViewPager pager;
		SlidingTabLayout slidingTabs;
		RecyclerView.RecycledViewPool recycledViewPool;
	}

	private ViewHolder holder;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live, container, false);

		holder = new ViewHolder();
		holder.pager = (ViewPager) view.findViewById(R.id.pager);
		holder.pager.setAdapter(new LivePagerAdapter(getChildFragmentManager(), getResources()));
		holder.slidingTabs = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
		holder.slidingTabs.setViewPager(holder.pager);
		holder.recycledViewPool = new RecyclerView.RecycledViewPool();

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		holder = null;
	}

	@Override
	public RecyclerView.RecycledViewPool getRecycledViewPool() {
		return (holder == null) ? null : holder.recycledViewPool;
	}

	private static class LivePagerAdapter extends FragmentPagerAdapter {

		private final Resources resources;

		public LivePagerAdapter(FragmentManager fm, Resources resources) {
			super(fm);
			this.resources = resources;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					return new NextLiveListFragment();
				case 1:
					return new NowLiveListFragment();
			}
			return null;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return resources.getString(R.string.next);
				case 1:
					return resources.getString(R.string.now);
			}
			return null;
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
