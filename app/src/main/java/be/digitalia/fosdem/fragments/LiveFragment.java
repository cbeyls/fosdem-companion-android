package be.digitalia.fosdem.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.utils.RecyclerViewUtils;

public class LiveFragment extends Fragment implements RecycledViewPoolProvider {

	static class ViewHolder {
		ViewPager2 pager;
		TabLayout tabs;
		RecyclerView.RecycledViewPool recycledViewPool;
	}

	private ViewHolder holder;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live, container, false);

		holder = new ViewHolder();
		holder.pager = view.findViewById(R.id.pager);
		final LivePagerAdapter adapter = new LivePagerAdapter(this);
		holder.pager.setAdapter(adapter);
		holder.pager.setOffscreenPageLimit(1);
		RecyclerViewUtils.enforceSingleScrollDirection(RecyclerViewUtils.getRecyclerView(holder.pager));
		holder.tabs = view.findViewById(R.id.tabs);
		new TabLayoutMediator(holder.tabs, holder.pager, false,
				(tab, position) -> tab.setText(adapter.getPageTitle(position))).attach();
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

	private static class LivePagerAdapter extends FragmentStateAdapter {

		private final Resources resources;

		LivePagerAdapter(Fragment fragment) {
			super(fragment.getChildFragmentManager(), fragment.getViewLifecycleOwner().getLifecycle());
			this.resources = fragment.getResources();
		}

		@Override
		public int getItemCount() {
			return 2;
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			switch (position) {
				case 0:
					return new NextLiveListFragment();
				case 1:
					return new NowLiveListFragment();
			}
			throw new IllegalStateException();
		}

		CharSequence getPageTitle(int position) {
			switch (position) {
				case 0:
					return resources.getString(R.string.next);
				case 1:
					return resources.getString(R.string.now);
			}
			return null;
		}
	}
}
