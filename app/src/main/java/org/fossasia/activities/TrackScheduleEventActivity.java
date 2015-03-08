package org.fossasia.activities;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;

import com.viewpagerindicator.PageIndicator;

import org.fossasia.R;
import org.fossasia.db.DatabaseManager;
import org.fossasia.fragments.EventDetailsFragment;
import org.fossasia.model.Track;
import org.fossasia.utils.NfcUtils;
import org.fossasia.utils.NfcUtils.CreateNfcAppDataCallback;

/**
 * Event view of the track schedule; allows to slide between events of the same track using a ViewPager.
 *
 * @author Christophe Beyls
 */
public class TrackScheduleEventActivity extends ActionBarActivity implements CreateNfcAppDataCallback {

    public static final String EXTRA_DAY = "day";
    public static final String EXTRA_TRACK = "track";
    public static final String EXTRA_POSITION = "position";

    private static final int EVENTS_LOADER_ID = 1;

    private Track track;
    private int initialPosition = -1;
    private View progress;
    private ViewPager pager;
    private PageIndicator pageIndicator;
    private TrackScheduleEventAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.track_schedule_event);

        Bundle extras = getIntent().getExtras();
//        day = extras.getParcelable(EXTRA_DAY);
        track = extras.getParcelable(EXTRA_TRACK);

        progress = findViewById(R.id.progress);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new TrackScheduleEventAdapter(getSupportFragmentManager());
        pageIndicator = (PageIndicator) findViewById(R.id.indicator);

        if (savedInstanceState == null) {
            initialPosition = extras.getInt(EXTRA_POSITION, -1);
            pager.setAdapter(adapter);
            pageIndicator.setViewPager(pager);
        }

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(track.toString());

        // Enable Android Beam
        NfcUtils.setAppDataPushMessageCallbackIfAvailable(this, this);

        setCustomProgressVisibility(true);
    }

    private void setCustomProgressVisibility(boolean isVisible) {
        progress.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public byte[] createNfcAppData() {
        if (adapter.getCount() == 0) {
            return null;
        }
        long eventId = adapter.getItemId(pager.getCurrentItem());
        if (eventId == -1L) {
            return null;
        }
        return String.valueOf(eventId).getBytes();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }


    public static class TrackScheduleEventAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;

        public TrackScheduleEventAdapter(FragmentManager fm) {
            super(fm);
        }

        public Cursor getCursor() {
            return cursor;
        }

        public void setCursor(Cursor cursor) {
            this.cursor = cursor;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return (cursor == null) ? 0 : cursor.getCount();
        }

        @Override
        public Fragment getItem(int position) {
            cursor.moveToPosition(position);
            return EventDetailsFragment.newInstance(DatabaseManager.toEvent(cursor));
        }

        public long getItemId(int position) {
            if (!cursor.moveToPosition(position)) {
                return -1L;
            }
            return cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        }
    }
}
