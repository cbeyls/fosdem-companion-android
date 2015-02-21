package org.fossasia.fragments;

import android.os.Bundle;

import org.fossasia.adapters.SpeakerAdapter;
import org.fossasia.db.DatabaseManager;

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


