package be.digitalia.fosdem.fragments;

import android.os.Bundle;

import be.digitalia.fosdem.adapters.SpeakerAdapter;
import be.digitalia.fosdem.db.DatabaseManager;

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

}


