package be.digitalia.fosdem.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import be.digitalia.fosdem.R;

public class LiveFragment extends Fragment implements RecycledViewPoolProvider {

	static class ViewHolder {
		ViewPager pager;
		TabLayout tabs;
		RecyclerView.RecycledViewPool recycledViewPool;
	}

	private ViewHolder holder;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live, container, false);

		holder = new ViewHolder();
		holder.pager = view.findViewById(R.id.pager);
		holder.pager.setAdapter(new LivePagerAdapter(getChildFragmentManager(), getResources()));
		holder.tabs = view.findViewById(R.id.tabs);
		holder.tabs.setupWithViewPager(holder.pager, false);
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

		@NonNull
		@Override
		public Object instantiateItem(@NonNull ViewGroup container, int position) {
			// Allow the non-primary fragments to start as soon as they are visible
			Fragment f = (Fragment) super.instantiateItem(container, position);
			f.setUserVisibleHint(true);
			return f;
		}
	}
}
