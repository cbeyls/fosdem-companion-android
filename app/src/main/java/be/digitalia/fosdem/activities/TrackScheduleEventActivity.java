package be.digitalia.fosdem.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomappbar.BottomAppBar;

import java.util.List;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.NfcUtils;
import be.digitalia.fosdem.utils.NfcUtils.CreateNfcAppDataCallback;
import be.digitalia.fosdem.utils.ThemeUtils;
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel;
import be.digitalia.fosdem.viewmodels.TrackScheduleEventViewModel;
import be.digitalia.fosdem.widgets.BookmarkStatusAdapter;
import be.digitalia.fosdem.widgets.ContentLoadingProgressBar;

/**
 * Event view of the track schedule; allows to slide between events of the same track using a ViewPager.
 *
 * @author Christophe Beyls
 */
public class TrackScheduleEventActivity extends AppCompatActivity implements Observer<List<Event>>, CreateNfcAppDataCallback {

	public static final String EXTRA_DAY = "day";
	public static final String EXTRA_TRACK = "track";
	public static final String EXTRA_POSITION = "position";

	private int initialPosition = -1;
	private ContentLoadingProgressBar progress;
	private ViewPager pager;
	TrackScheduleEventAdapter adapter;

	BookmarkStatusViewModel bookmarkStatusViewModel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.track_schedule_event);
		AppBarLayout appBarLayout = findViewById(R.id.appbar);
		Toolbar toolbar = findViewById(R.id.toolbar);
		BottomAppBar bottomAppBar = findViewById(R.id.bottom_appbar);
		setSupportActionBar(bottomAppBar);

		Bundle extras = getIntent().getExtras();
		final Day day = extras.getParcelable(EXTRA_DAY);
		final Track track = extras.getParcelable(EXTRA_TRACK);

		progress = findViewById(R.id.progress);
		pager = findViewById(R.id.pager);
		adapter = new TrackScheduleEventAdapter(getSupportFragmentManager());

		if (savedInstanceState == null) {
			initialPosition = extras.getInt(EXTRA_POSITION, -1);
		}

		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
		toolbar.setNavigationContentDescription(R.string.abc_action_bar_up_description);
		toolbar.setNavigationOnClickListener(v -> onSupportNavigateUp());
		toolbar.setTitle(track.toString());
		toolbar.setSubtitle(day.toString());
		setTitle(String.format("%1$s, %2$s", track.toString(), day.toString()));
		ThemeUtils.setStatusBarTrackColor(this, track.getType());
		final ColorStateList trackColor = ContextCompat.getColorStateList(this, track.getType().getColorResId());
		ThemeUtils.setAppBarLayoutBackgroundColor(appBarLayout, trackColor);
		bottomAppBar.setBackgroundTint(trackColor);

		// Monitor the currently displayed event to update the bookmark status in FAB
		ImageButton floatingActionButton = findViewById(R.id.fab);
		bookmarkStatusViewModel = ViewModelProviders.of(this).get(BookmarkStatusViewModel.class);
		BookmarkStatusAdapter.setupWithImageButton(bookmarkStatusViewModel, this, floatingActionButton);
		pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				bookmarkStatusViewModel.setEvent(adapter.getEvent(position));
			}
		});

		setCustomProgressVisibility(true);
		final TrackScheduleEventViewModel viewModel = ViewModelProviders.of(this).get(TrackScheduleEventViewModel.class);
		viewModel.setTrack(day, track);
		viewModel.getScheduleSnapshot().observe(this, this);

		// Enable Android Beam
		NfcUtils.setAppDataPushMessageCallbackIfAvailable(this, this);
	}

	private void setCustomProgressVisibility(boolean isVisible) {
		if (isVisible) {
			progress.show();
		} else {
			progress.hide();
		}
	}

	@Nullable
	@Override
	public Intent getSupportParentActivityIntent() {
		final Event event = bookmarkStatusViewModel.getEvent();
		if (event == null) {
			return null;
		}
		// Navigate up to the track associated with this event
		return new Intent(this, TrackScheduleActivity.class)
				.putExtra(TrackScheduleActivity.EXTRA_DAY, event.getDay())
				.putExtra(TrackScheduleActivity.EXTRA_TRACK, event.getTrack())
				.putExtra(TrackScheduleActivity.EXTRA_FROM_EVENT_ID, event.getId());
	}

	@Override
	public NdefRecord createNfcAppData() {
		final Event event = bookmarkStatusViewModel.getEvent();
		if (event == null) {
			return null;
		}
		return NfcUtils.createEventAppData(this, event);
	}

	@Override
	public void onChanged(List<Event> schedule) {
		setCustomProgressVisibility(false);

		if (schedule != null) {
			pager.setVisibility(View.VISIBLE);
			adapter.setSchedule(schedule);

			// Delay setting the adapter
			// to ensure the current position is restored properly
			if (pager.getAdapter() == null) {
				pager.setAdapter(adapter);

				if (initialPosition != -1) {
					pager.setCurrentItem(initialPosition, false);
					initialPosition = -1;
				}

				final int currentPosition = pager.getCurrentItem();
				if (currentPosition >= 0) {
					bookmarkStatusViewModel.setEvent(adapter.getEvent(currentPosition));
				}
			}
		}
	}

	private static class TrackScheduleEventAdapter extends FragmentStatePagerAdapter {

		private List<Event> events = null;

		TrackScheduleEventAdapter(FragmentManager fm) {
			super(fm);
		}

		public void setSchedule(List<Event> schedule) {
			this.events = schedule;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return (events == null) ? 0 : events.size();
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			return EventDetailsFragment.newInstance(events.get(position));
		}

		public Event getEvent(int position) {
			if (position < 0 || position >= getCount()) {
				return null;
			}
			return events.get(position);
		}
	}
}
