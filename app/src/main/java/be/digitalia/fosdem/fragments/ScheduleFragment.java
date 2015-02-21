package be.digitalia.fosdem.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.adapters.ScheduleAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.FossasiaEvent;

/**
 * Created by Abhishek on 20/02/15.
 */
public class ScheduleFragment extends SmoothListFragment {

    private ArrayList<FossasiaEvent> events;

    public static Fragment newInstance(int day) {
        Fragment fragment = new ScheduleFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("DAY", day);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatabaseManager dbManager = DatabaseManager.getInstance();
        events = dbManager.getSchedule();
        setListAdapter(new ScheduleAdapter(getActivity(), events));

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        long idNew = (long) v.getTag();
        Toast.makeText(getActivity(), "Position: " + idNew, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity().getApplicationContext(), EventDetailsActivity.class);
        intent.putExtra("event", events.get((int) idNew));
        startActivity(intent);
        super.onListItemClick(l, v, position, id);
    }
}
