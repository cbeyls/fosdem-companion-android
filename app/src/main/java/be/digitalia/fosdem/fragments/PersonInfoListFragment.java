package be.digitalia.fosdem.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.EventDetailsActivity;
import be.digitalia.fosdem.adapters.EventsAdapter;
import be.digitalia.fosdem.adapters.ScheduleAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.model.FossasiaEvent;
import be.digitalia.fosdem.model.KeySpeaker;

public class PersonInfoListFragment extends SmoothListFragment {

	private static final int PERSON_EVENTS_LOADER_ID = 1;
	private static final String ARG_PERSON = "person";

    private KeySpeaker person;
    private EventsAdapter adapter;
    private ArrayList<FossasiaEvent> events;

    public static PersonInfoListFragment newInstance(KeySpeaker speaker) {
        PersonInfoListFragment f = new PersonInfoListFragment();
		Bundle args = new Bundle();
        args.putParcelable(ARG_PERSON, speaker);
        f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		person = getArguments().getParcelable(ARG_PERSON);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.person, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.more_info:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(person.getLinkedInUrl()));
                startActivity(intent);
				return true;
		}
		return false;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getString(R.string.no_data));

		int contentMargin = getResources().getDimensionPixelSize(R.dimen.content_margin);
		ListView listView = getListView();
		listView.setClipToPadding(false);
		listView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);

		View headerView = LayoutInflater.from(getActivity()).inflate(R.layout.header_person_info, null);

        TextView name;
        TextView designation;
        NetworkImageView speakerImage;
        TextView information;
        ImageView linkedIn;
        ImageView twitter;

        name = (TextView) headerView.findViewById(R.id.textView_speaker_name);
        designation = (TextView) headerView.findViewById(R.id.textView_speaker_designation);
        speakerImage = (NetworkImageView) headerView.findViewById(R.id.imageView_speaker_pic);
        speakerImage.setDefaultImageResId(R.drawable.default_user);
        information = (TextView) headerView.findViewById(R.id.textView_speaker_information);
        linkedIn = (ImageView) headerView.findViewById(R.id.imageView_linkedin);
        twitter = (ImageView) headerView.findViewById(R.id.imageView_twitter);
        getListView().addHeaderView(headerView, null, false);
        name.setText(person.getName());
        designation.setText(person.getDesignation());
        information.setText(person.getInformation());
        if (person.getLinkedInUrl() == null) {
            linkedIn.setVisibility(View.GONE);
        }
        if (person.getTwitterHandle() == null) {
            twitter.setVisibility(View.GONE);
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        events = dbManager.getEventBySpeaker(person.getName());
        setListAdapter(new ScheduleAdapter(getActivity(), events));
        ;

	}


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(getActivity().getApplicationContext(), EventDetailsActivity.class);
        // Using position - 1 because first 0th position is taken by Header View
        intent.putExtra("event", events.get(position - 1));
        startActivity(intent);
        super.onListItemClick(l, v, position, id);
    }

}
