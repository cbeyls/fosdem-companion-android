package org.fossasia.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.common.view.SlidingTabLayout;

import org.fossasia.R;
import org.fossasia.db.DatabaseManager;
import org.fossasia.model.Day;

import java.util.ArrayList;


/**
 * Created by Abhishek on 24/02/15.
 */
public class ScheduleFragment extends Fragment {

    public final static String TAG = "ScheduleFragment";

    private DayLoader daysAdapter;
    private ViewHolder holder;

    public static Fragment newInstance(String track) {
        ScheduleFragment fragment = new ScheduleFragment();
        Bundle bundle = new Bundle();
        bundle.putString("TRACK", track);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String track = getArguments().getString("TRACK");
            DatabaseManager db = DatabaseManager.getInstance();

            ArrayList<Day> days = db.getDates(track);
            ArrayList<Day> staticDays = new ArrayList<>();
            staticDays.add(new Day(0, "March 13"));
            staticDays.add(new Day(1, "March 14"));
            staticDays.add(new Day(2, "March 15"));
            String subTitle = "";
            for(Day day : days) {
                if(days.indexOf(day) != 0) {
                    subTitle += ", ";
                }
                subTitle += day.getDate();

            }
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(subTitle);
            daysAdapter = new DayLoader(getChildFragmentManager(), track, staticDays);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        holder = new ViewHolder();
        holder.contentView = view.findViewById(R.id.content);
        holder.emptyView = view.findViewById(android.R.id.empty);
        holder.pager = (ViewPager) view.findViewById(R.id.pager);
        holder.slidingTabs = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        holder.contentView.setVisibility(View.VISIBLE);
        holder.emptyView.setVisibility(View.GONE);
        if (holder.pager.getAdapter() == null) {
            holder.pager.setAdapter(daysAdapter);
        }
        holder.slidingTabs.setViewPager(holder.pager);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        holder = null;
    }

    private static class ViewHolder {
        View contentView;
        View emptyView;
        ViewPager pager;
        SlidingTabLayout slidingTabs;
    }

    private static class DayLoader extends FragmentStatePagerAdapter {

        private ArrayList<String> mPageTitle;
        private ArrayList<Day> days;
        private String track;

        public DayLoader(FragmentManager fm, String track, ArrayList<Day> days) {
            super(fm);
            mPageTitle = new ArrayList<String>();
            this.track = track;
            this.days = days;

        }

        @Override
        public Fragment getItem(int position) {
            return ScheduleListFragment.newInstance(days.get(position).getDate(), track);
        }

        @Override
        public int getCount() {
            return days.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return days.get(position).getDate();
        }
    }
}
