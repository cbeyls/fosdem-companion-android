package be.digitalia.fosdem.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomappbar.BottomAppBar;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.NfcUtils;
import be.digitalia.fosdem.utils.NfcUtils.CreateNfcAppDataCallback;
import be.digitalia.fosdem.utils.RecyclerViewUtils;
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
	private ViewPager2 pager;
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
		RecyclerViewUtils.enforceSingleScrollDirection(RecyclerViewUtils.getRecyclerView(pager));
		adapter = new TrackScheduleEventAdapter(this);

		if (savedInstanceState == null) {
			initialPosition = extras.getInt(EXTRA_POSITION, -1);
		}

		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
		toolbar.setNavigationContentDescription(R.string.abc_action_bar_up_description);
		toolbar.setNavigationOnClickListener(v -> onSupportNavigateUp());
		toolbar.setTitle(track.toString());
		toolbar.setSubtitle(day.toString());
		setTitle(String.format("%1$s, %2$s", track.toString(), day.toString()));
		final Track.Type trackType = track.getType();
		if (ThemeUtils.isLightTheme(this)) {
			final ColorStateList trackAppBarColor = ContextCompat.getColorStateList(this, trackType.getAppBarColorResId());
			final int trackStatusBarColor = ContextCompat.getColor(this, trackType.getStatusBarColorResId());
			ThemeUtils.setActivityColors(this, trackAppBarColor.getDefaultColor(), trackStatusBarColor);
			ThemeUtils.tintBackground(appBarLayout, trackAppBarColor);
		} else {
			final ColorStateList trackTextColor = ContextCompat.getColorStateList(this, trackType.getTextColorResId());
			toolbar.setTitleTextColor(trackTextColor);
		}

		final ViewModelProvider viewModelProvider = new ViewModelProvider(this);

		// Monitor the currently displayed event to update the bookmark status in FAB
		ImageButton floatingActionButton = findViewById(R.id.fab);
		bookmarkStatusViewModel = viewModelProvider.get(BookmarkStatusViewModel.class);
		BookmarkStatusAdapter.setupWithImageButton(bookmarkStatusViewModel, this, floatingActionButton);
		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				bookmarkStatusViewModel.setEvent(adapter.getEvent(position));
			}
		});

		setCustomProgressVisibility(true);
		final TrackScheduleEventViewModel viewModel = viewModelProvider.get(TrackScheduleEventViewModel.class);
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

	private static class TrackScheduleEventAdapter extends FragmentStateAdapter {

		private List<Event> events = null;

		TrackScheduleEventAdapter(@NonNull FragmentActivity fragmentActivity) {
			super(fragmentActivity);
		}

		public void setSchedule(List<Event> schedule) {
			this.events = schedule;
			notifyDataSetChanged();
		}

		@Override
		public int getItemCount() {
			return (events == null) ? 0 : events.size();
		}

		@Override
		public long getItemId(int position) {
			return events.get(position).getId();
		}

		@Override
		public boolean containsItem(long itemId) {
			final int count = getItemCount();
			for (int i = 0; i < count; ++i) {
				if (events.get(i).getId() == itemId) {
					return true;
				}
			}
			return false;
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			final Fragment f = EventDetailsFragment.newInstance(events.get(position));
			// Workaround for duplicate menu items bug
			f.setMenuVisibility(false);
			return f;
		}

		public Event getEvent(int position) {
			if (position < 0 || position >= getItemCount()) {
				return null;
			}
			return events.get(position);
		}
	}
}
