package be.digitalia.fosdem.fragments;

/**
 * Created by Abhishek on 01/03/15.
 */

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import be.digitalia.fosdem.activities.PersonInfoActivity;
import be.digitalia.fosdem.adapters.SpeakerAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.Speaker;

public class SpeakerFragment extends SmoothListFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatabaseManager dbManager = DatabaseManager.getInstance();
        SpeakerAdapter adapter = new SpeakerAdapter(getActivity().getApplicationContext(), dbManager.getSpeakers(false));
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Speaker speaker = (Speaker) v.getTag();
        Intent intent = new Intent(getActivity().getApplicationContext(), PersonInfoActivity.class);
        intent.putExtra(PersonInfoActivity.SPEAKER, speaker);
        startActivity(intent);
    }
}


