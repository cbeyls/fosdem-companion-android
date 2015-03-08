package org.fossasia.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.fossasia.R;
import org.fossasia.db.DatabaseManager;
import org.fossasia.model.Day;
import org.fossasia.model.Event;
import org.fossasia.model.Track;
import org.fossasia.utils.DateUtils;

import java.text.DateFormat;

public class TrackScheduleListFragment extends SmoothListFragment implements Handler.Callback {

    private static final int EVENTS_LOADER_ID = 1;
    private static final int REFRESH_TIME_WHAT = 1;
    private static final long REFRESH_TIME_INTERVAL = 60 * 1000L; // 1min
    private static final String ARG_DAY = "day";
    private static final String ARG_TRACK = "track";
    private static final String ARG_FROM_EVENT_ID = "from_event_id";
    private Day day;
    private Handler handler;
    private TrackScheduleAdapter adapter;
    private Callbacks listener;
    private boolean selectionEnabled = false;
    private boolean isListAlreadyShown = false;

    public static TrackScheduleListFragment newInstance(Day day, Track track) {
        TrackScheduleListFragment f = new TrackScheduleListFragment();
        Bundle args = new Bundle();
//        args.putParcelable(ARG_DAY, day);
        args.putParcelable(ARG_TRACK, track);
        f.setArguments(args);
        return f;
    }

    public static TrackScheduleListFragment newInstance(Day day, Track track, long fromEventId) {
        TrackScheduleListFragment f = new TrackScheduleListFragment();
        Bundle args = new Bundle();
//        args.putParcelable(ARG_DAY, day);
        args.putParcelable(ARG_TRACK, track);
        args.putLong(ARG_FROM_EVENT_ID, fromEventId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        day = getArguments().getParcelable(ARG_DAY);
        handler = new Handler(this);
        adapter = new TrackScheduleAdapter(getActivity());
        setListAdapter(adapter);

        if (savedInstanceState != null) {
            isListAlreadyShown = savedInstanceState.getBoolean("isListAlreadyShown");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isListAlreadyShown", isListAlreadyShown);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Callbacks) {
            listener = (Callbacks) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    private void notifyEventSelected(int position) {
        if (listener != null) {
            listener.onEventSelected(position, (position == ListView.INVALID_POSITION) ? null : adapter.getItem(position));
        }
    }

    public void setSelectionEnabled(boolean selectionEnabled) {
        this.selectionEnabled = selectionEnabled;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setChoiceMode(selectionEnabled ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
        setEmptyText(getString(R.string.no_data));
        setListShown(false);

    }

    @Override
    public void onStart() {
        super.onStart();

//        // Setup display auto-refresh during the track's day
//        long now = System.currentTimeMillis();
////        long dayStart = day.getDate().getTime();
//        if (now < dayStart) {
//            // Before track day, schedule refresh in the future
//            handler.sendEmptyMessageDelayed(REFRESH_TIME_WHAT, dayStart - now);
//        } else if (now < dayStart + android.text.format.DateUtils.DAY_IN_MILLIS) {
//            // During track day, start refresh immediately
//            adapter.setCurrentTime(now);
//            handler.sendEmptyMessageDelayed(REFRESH_TIME_WHAT, REFRESH_TIME_INTERVAL);
//        } else {
//            // After track day, disable refresh
//            adapter.setCurrentTime(-1L);
//        }
    }

    @Override
    public void onStop() {
        handler.removeMessages(REFRESH_TIME_WHAT);
        super.onStop();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case REFRESH_TIME_WHAT:
                adapter.setCurrentTime(System.currentTimeMillis());
                handler.sendEmptyMessageDelayed(REFRESH_TIME_WHAT, REFRESH_TIME_INTERVAL);
                return true;
        }
        return false;
    }




    /**
     * @return The default position in the list, or -1 if the list is empty
     */
    private int getDefaultPosition() {
        final int count = adapter.getCount();
        if (count == 0) {
            return ListView.INVALID_POSITION;
        }
        long fromEventId = getArguments().getLong(ARG_FROM_EVENT_ID, -1L);
        if (fromEventId != -1L) {
            // Look for the source event in the list and return its position
            for (int i = 0; i < count; ++i) {
                if (adapter.getItemId(i) == fromEventId) {
                    return i;
                }
            }
        }
        return 0;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        notifyEventSelected(position);
    }

    /**
     * Interface implemented by container activities
     */
    public interface Callbacks {
        void onEventSelected(int position, Event event);
    }

    private static class TrackScheduleAdapter extends CursorAdapter {

        private static final DateFormat TIME_DATE_FORMAT = DateUtils.getTimeDateFormat();

        private final LayoutInflater inflater;
        private final int timeBackgroundColor;
        private final int timeForegroundColor;
        private final int timeRunningBackgroundColor;
        private final int timeRunningForegroundColor;
        private final int titleTextSize;
        private long currentTime = -1L;

        public TrackScheduleAdapter(Context context) {
            super(context, null, 0);
            inflater = LayoutInflater.from(context);
            Resources res = context.getResources();
            timeBackgroundColor = res.getColor(R.color.schedule_time_background);
            timeForegroundColor = res.getColor(R.color.schedule_time_foreground);
            timeRunningBackgroundColor = res.getColor(R.color.schedule_time_running_background);
            timeRunningForegroundColor = res.getColor(R.color.schedule_time_running_foreground);
            titleTextSize = res.getDimensionPixelSize(R.dimen.list_item_title_text_size);
        }

        public void setCurrentTime(long time) {
            if (currentTime != time) {
                currentTime = time;
                notifyDataSetChanged();
            }
        }

        @Override
        public Event getItem(int position) {
            return DatabaseManager.toEvent((Cursor) super.getItem(position));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = inflater.inflate(R.layout.item_schedule_event, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.time = (TextView) view.findViewById(R.id.time);
            holder.text = (TextView) view.findViewById(R.id.text);
            holder.titleSizeSpan = new AbsoluteSizeSpan(titleTextSize);
            holder.boldStyleSpan = new StyleSpan(Typeface.BOLD);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            Event event = DatabaseManager.toEvent(cursor, holder.event);
            holder.event = event;

            holder.time.setText(TIME_DATE_FORMAT.format(event.getStartTime()));
            if ((currentTime != -1L) && event.isRunningAtTime(currentTime)) {
                // Contrast colors for running event
                holder.time.setBackgroundColor(timeRunningBackgroundColor);
                holder.time.setTextColor(timeRunningForegroundColor);
            } else {
                // Normal colors
                holder.time.setBackgroundColor(timeBackgroundColor);
                holder.time.setTextColor(timeForegroundColor);
            }

            SpannableString spannableString;
            String eventTitle = event.getTitle();
            String personsSummary = event.getPersonsSummary();
            if (TextUtils.isEmpty(personsSummary)) {
                spannableString = new SpannableString(String.format("%1$s\n%2$s", eventTitle, event.getRoomName()));
            } else {
                spannableString = new SpannableString(String.format("%1$s\n%2$s\n%3$s", eventTitle, personsSummary, event.getRoomName()));
            }
            spannableString.setSpan(holder.titleSizeSpan, 0, eventTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(holder.boldStyleSpan, 0, eventTitle.length() + personsSummary.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            holder.text.setText(spannableString);
            int bookmarkDrawable = DatabaseManager.toBookmarkStatus(cursor) ? R.drawable.ic_bookmark_grey600_24dp : 0;
            holder.text.setCompoundDrawablesWithIntrinsicBounds(0, 0, bookmarkDrawable, 0);
        }

        private static class ViewHolder {
            TextView time;
            TextView text;
            AbsoluteSizeSpan titleSizeSpan;
            StyleSpan boldStyleSpan;
            Event event;
        }
    }
}
