package be.digitalia.fosdem.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import be.digitalia.fosdem.activities.PersonInfoActivity;
import be.digitalia.fosdem.adapters.SpeakerAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.KeySpeaker;

/**
 * Created by Abhishek on 14/02/15.
 */
public class KeySpeakerFragment extends SmoothListFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatabaseManager dbManager = DatabaseManager.getInstance();
        SpeakerAdapter adapter = new SpeakerAdapter(getActivity().getApplicationContext(), dbManager.getKeySpeakers());
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        KeySpeaker speaker = (KeySpeaker) v.getTag();
        Intent intent = new Intent(getActivity().getApplicationContext(), PersonInfoActivity.class);
        intent.putExtra(PersonInfoActivity.SPEAKER, speaker);
        startActivity(intent);
    }
}


