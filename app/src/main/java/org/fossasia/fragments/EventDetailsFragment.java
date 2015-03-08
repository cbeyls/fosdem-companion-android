package org.fossasia.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.fossasia.R;
import org.fossasia.db.DatabaseManager;
import org.fossasia.model.Building;
import org.fossasia.model.Event;
import org.fossasia.model.Link;
import org.fossasia.model.Person;
import org.fossasia.utils.DateUtils;
import org.fossasia.utils.StringUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class EventDetailsFragment extends Fragment {

    private static final int BOOKMARK_STATUS_LOADER_ID = 1;
    private static final int EVENT_DETAILS_LOADER_ID = 2;
    private static final String ARG_EVENT = "event";
    private static final DateFormat TIME_DATE_FORMAT = DateUtils.getTimeDateFormat();
    private Event event;
    private int personsCount = 1;
    private Boolean isBookmarked;
    private final View.OnClickListener actionButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (isBookmarked != null) {
                new UpdateBookmarkAsyncTask(event).execute(isBookmarked);
            }
        }
    };
    private ViewHolder holder;
    private MenuItem bookmarkMenuItem;
    private ImageView actionButton;

    public static EventDetailsFragment newInstance(Event event) {
        EventDetailsFragment f = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_EVENT, event);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        event = getArguments().getParcelable(ARG_EVENT);
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        holder = new ViewHolder();
        holder.inflater = inflater;

        ((TextView) view.findViewById(R.id.title)).setText(event.getTitle());
        TextView textView = (TextView) view.findViewById(R.id.subtitle);
        String text = event.getSubTitle();
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
        }

        MovementMethod linkMovementMethod = LinkMovementMethod.getInstance();

        // Set the persons summary text first; replace it with the clickable text when the loader completes
        holder.personsTextView = (TextView) view.findViewById(R.id.persons);
        String personsSummary = event.getPersonsSummary();
        if (TextUtils.isEmpty(personsSummary)) {
            holder.personsTextView.setVisibility(View.GONE);
        } else {
            holder.personsTextView.setText(personsSummary);
            holder.personsTextView.setMovementMethod(linkMovementMethod);
            holder.personsTextView.setVisibility(View.VISIBLE);
        }

        ((TextView) view.findViewById(R.id.track)).setText(event.getTrack().getName());
        Date startTime = event.getStartTime();
        Date endTime = event.getEndTime();
        ((TextView) view.findViewById(R.id.time)).setText(text);
        final String roomName = event.getRoomName();
        TextView roomTextView = (TextView) view.findViewById(R.id.room);
        Spannable roomText = new SpannableString(String.format("%1$s (Building %2$s)", roomName, Building.fromRoomName(roomName)));
        final int roomImageResId = getResources().getIdentifier(StringUtils.roomNameToResourceName(roomName), "drawable", getActivity().getPackageName());
        // If the room image exists, make the room text clickable to display it
        if (roomImageResId != 0) {
            roomText.setSpan(new UnderlineSpan(), 0, roomText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            roomTextView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    RoomImageDialogFragment.newInstance(roomName, roomImageResId).show(getFragmentManager());
                }
            });
            roomTextView.setFocusable(true);
        }
        roomTextView.setText(roomText);

        textView = (TextView) view.findViewById(R.id.abstract_text);
        text = event.getAbstractText();
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(StringUtils.trimEnd(Html.fromHtml(text)));
            textView.setMovementMethod(linkMovementMethod);
        }
        textView = (TextView) view.findViewById(R.id.description);
        text = event.getDescription();
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(StringUtils.trimEnd(Html.fromHtml(text)));
            textView.setMovementMethod(linkMovementMethod);
        }

        holder.linksContainer = (ViewGroup) view.findViewById(R.id.links_container);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        if (activity instanceof FloatingActionButtonProvider) {
            actionButton = ((FloatingActionButtonProvider) activity).getActionButton();
            if (actionButton != null) {
                actionButton.setOnClickListener(actionButtonClickListener);
            }
        }

        // Ensure the actionButton is initialized before creating the options menu
        setHasOptionsMenu(true);

        LoaderManager loaderManager = getLoaderManager();
//		loaderManager.initLoader(BOOKMARK_STATUS_LOADER_ID, null, bookmarkStatusLoaderCallbacks);
//		loaderManager.initLoader(EVENT_DETAILS_LOADER_ID, null, eventDetailsLoaderCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        holder = null;
        if (actionButton != null) {
            // Clear the reference to this fragment
            actionButton.setOnClickListener(null);
            actionButton = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.event, menu);
        menu.findItem(R.id.share).setIntent(getShareChooserIntent());
        bookmarkMenuItem = menu.findItem(R.id.bookmark);
        if (actionButton != null) {
            bookmarkMenuItem.setEnabled(false).setVisible(false);
        }
        updateOptionsMenu();
    }

    private Intent getShareChooserIntent() {
        return ShareCompat.IntentBuilder.from(getActivity())
                .setSubject(String.format("%1$s (FOSDEM)", event.getTitle()))
                .setType("text/plain")
                .setText(String.format("%1$s %2$s #FOSDEM", event.getTitle(), event.getUrl()))
                .setChooserTitle(R.string.share)
                .createChooserIntent();
    }

    private void updateOptionsMenu() {
        if (actionButton != null) {
            // Action Button is used as bookmark button

            if (isBookmarked == null) {
                actionButton.setEnabled(false);
            } else {
                actionButton.setEnabled(true);

                if (isBookmarked) {
                    actionButton.setContentDescription(getString(R.string.remove_bookmark));
                    actionButton.setImageResource(R.drawable.ic_bookmark_white_24dp);
                } else {
                    actionButton.setContentDescription(getString(R.string.add_bookmark));
                    actionButton.setImageResource(R.drawable.ic_bookmark_outline_white_24dp);
                }
            }
        } else {
            // Standard menu item is used as bookmark button

            if (bookmarkMenuItem != null) {
                if (isBookmarked == null) {
                    bookmarkMenuItem.setEnabled(false);
                } else {
                    bookmarkMenuItem.setEnabled(true);

                    if (isBookmarked) {
                        bookmarkMenuItem.setTitle(R.string.remove_bookmark);
                        bookmarkMenuItem.setIcon(R.drawable.ic_bookmark_white_24dp);
                    } else {
                        bookmarkMenuItem.setTitle(R.string.add_bookmark);
                        bookmarkMenuItem.setIcon(R.drawable.ic_bookmark_outline_white_24dp);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();
        bookmarkMenuItem = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bookmark:
                if (isBookmarked != null) {
                    new UpdateBookmarkAsyncTask(event).execute(isBookmarked);
                }
                return true;
            case R.id.add_to_agenda:
//				addToAgenda();
                return true;
        }
        return false;
    }

    /**
     * Interface implemented by container activities
     */
    public interface FloatingActionButtonProvider {
        // May return null
        ImageView getActionButton();
    }

    private static class EventDetails {
        List<Person> persons;
        List<Link> links;
    }

    private static class ViewHolder {
        LayoutInflater inflater;
        TextView personsTextView;
        ViewGroup linksContainer;
    }

    private static class UpdateBookmarkAsyncTask extends AsyncTask<Boolean, Void, Void> {

        private final Event event;

        public UpdateBookmarkAsyncTask(Event event) {
            this.event = event;
        }

        @Override
        protected Void doInBackground(Boolean... remove) {
            if (remove[0]) {
                DatabaseManager.getInstance().removeBookmark(event);
            } else {
                DatabaseManager.getInstance().addBookmark(event);
            }
            return null;
        }
    }

}