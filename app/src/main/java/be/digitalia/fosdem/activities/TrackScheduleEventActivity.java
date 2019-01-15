package be.digitalia.fosdem.activities;

import android.os.Bundle;

import com.viewpagerindicator.UnderlinePageIndicator;

import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.StatusEvent;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.NfcUtils;
import be.digitalia.fosdem.utils.NfcUtils.CreateNfcAppDataCallback;
import be.digitalia.fosdem.utils.ThemeUtils;
import be.digitalia.fosdem.viewmodels.TrackScheduleViewModel;
import be.digitalia.fosdem.widgets.ContentLoadingProgressBar;

/**
 * Event view of the track schedule; allows to slide between events of the same track using a ViewPager.
 *
 * @author Christophe Beyls
 */
public class TrackScheduleEventActivity extends AppCompatActivity implements Observer<List<StatusEvent>>, CreateNfcAppDataCallback {

	public static final String EXTRA_DAY = "day";
	public static final String EXTRA_TRACK = "track";
	public static final String EXTRA_POSITION = "position";

	private Day day;
	private Track track;
	private int initialPosition = -1;
	private ContentLoadingProgressBar progress;
	private ViewPager pager;
	private UnderlinePageIndicator pageIndicator;
	private TrackScheduleEventAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.track_schedule_event);

		Bundle extras = getIntent().getExtras();
		day = extras.getParcelable(EXTRA_DAY);
		track = extras.getParcelable(EXTRA_TRACK);

		progress = findViewById(R.id.progress);
		pager = findViewById(R.id.pager);
		adapter = new TrackScheduleEventAdapter(getSupportFragmentManager());
		pageIndicator = findViewById(R.id.indicator);
		pageIndicator.setSelectedColor(ContextCompat.getColor(this, track.getType().getColorResId()));

		if (savedInstanceState == null) {
			initialPosition = extras.getInt(EXTRA_POSITION, -1);
			pager.setAdapter(adapter);
			pageIndicator.setViewPager(pager);
		}

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(track.toString());
		bar.setSubtitle(day.toString());
		ThemeUtils.setActionBarTrackColor(this, track.getType());

		// Enable Android Beam
		NfcUtils.setAppDataPushMessageCallbackIfAvailable(this, this);

		setCustomProgressVisibility(true);
		final TrackScheduleViewModel viewModel = ViewModelProviders.of(this).get(TrackScheduleViewModel.class);
		viewModel.setTrack(day, track);
		viewModel.getSchedule().observe(this, this);
	}

	private void setCustomProgressVisibility(boolean isVisible) {
		if (isVisible) {
			progress.show();
		} else {
			progress.hide();
		}
	}

	@Override
	public byte[] createNfcAppData() {
		if (adapter.getCount() == 0) {
			return null;
		}
		Event event = adapter.getEvent(pager.getCurrentItem());
		if (event == null) {
			return null;
		}
		return String.valueOf(event.getId()).getBytes();
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	@Override
	public void onChanged(List<StatusEvent> schedule) {
		setCustomProgressVisibility(false);

		if (schedule != null) {
			adapter.setSchedule(schedule);

			// Delay setting the adapter when the instance state is restored
			// to ensure the current position is restored properly
			if (pager.getAdapter() == null) {
				pager.setAdapter(adapter);
				pageIndicator.setViewPager(pager);
			}

			if (initialPosition != -1) {
				pager.setCurrentItem(initialPosition, false);
				initialPosition = -1;
			}
		}
	}

	public static class TrackScheduleEventAdapter extends FragmentStatePagerAdapter {

		private List<StatusEvent> events = null;

		public TrackScheduleEventAdapter(FragmentManager fm) {
			super(fm);
		}

		public void setSchedule(List<StatusEvent> schedule) {
			this.events = schedule;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return (events == null) ? 0 : events.size();
		}

		@Override
		public Fragment getItem(int position) {
			return EventDetailsFragment.newInstance(events.get(position).getEvent());
		}

		public Event getEvent(int position) {
			if (position < 0 || position >= getCount()) {
				return null;
			}
			return events.get(position).getEvent();
		}
	}
}
