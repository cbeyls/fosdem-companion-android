package be.digitalia.fosdem.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import be.digitalia.fosdem.adapters.ScheduleAdapter;
import be.digitalia.fosdem.db.DatabaseManager;

/**
 * Created by Abhishek on 20/02/15.
 */
public class ScheduleFragment extends SmoothListFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatabaseManager dbManager = DatabaseManager.getInstance();
        setListAdapter(new ScheduleAdapter(getActivity(), dbManager.getSchedule()));

    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getListView().setDividerHeight(20);
        getListView().setDivider(new ColorDrawable(Color.parseColor("#00000000")));
        getListView().setPadding(0, 10, 0, 0);

    }
}
