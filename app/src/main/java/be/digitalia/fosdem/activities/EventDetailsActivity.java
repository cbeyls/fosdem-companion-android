package be.digitalia.fosdem.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.utils.NfcUtils;
import be.digitalia.fosdem.utils.NfcUtils.CreateNfcAppDataCallback;
import be.digitalia.fosdem.utils.ThemeUtils;
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel;
import be.digitalia.fosdem.viewmodels.EventViewModel;
import be.digitalia.fosdem.widgets.BookmarkStatusAdapter;

/**
 * Displays a single event passed either as a complete Parcelable object in extras or as an id in data.
 *
 * @author Christophe Beyls
 */
public class EventDetailsActivity extends AppCompatActivity implements Observer<Event>, CreateNfcAppDataCallback {

	public static final String EXTRA_EVENT = "event";

	private AppBarLayout appBarLayout;
	private Toolbar toolbar;

	private BookmarkStatusViewModel bookmarkStatusViewModel;
	private Event event;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_event);
		appBarLayout = findViewById(R.id.appbar);
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(findViewById(R.id.bottom_appbar));

		ImageButton floatingActionButton = findViewById(R.id.fab);
		final ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		bookmarkStatusViewModel = viewModelProvider.get(BookmarkStatusViewModel.class);
		BookmarkStatusAdapter.setupWithImageButton(bookmarkStatusViewModel, this, floatingActionButton);

		Event event = getIntent().getParcelableExtra(EXTRA_EVENT);

		if (event != null) {
			// The event has been passed as parameter, it can be displayed immediately
			initEvent(event);
			if (savedInstanceState == null) {
				Fragment f = EventDetailsFragment.newInstance(event);
				getSupportFragmentManager().beginTransaction().add(R.id.content, f).commit();
			}
		} else {
			// Load the event from the DB using its id
			EventViewModel viewModel = viewModelProvider.get(EventViewModel.class);
			if (!viewModel.hasEventId()) {
				Intent intent = getIntent();
				String eventIdString;
				if (NfcUtils.hasAppData(intent)) {
					// NFC intent
					eventIdString = NfcUtils.toEventIdString((NfcUtils.extractAppData(intent)));
				} else {
					// Normal in-app intent
					eventIdString = intent.getDataString();
				}
				viewModel.setEventId(Long.parseLong(eventIdString));
			}
			viewModel.getEvent().observe(this, this);
		}
	}

	@Override
	public void onChanged(@Nullable Event event) {
		if (event == null) {
			// Event not found, quit
			Toast.makeText(this, getString(R.string.event_not_found_error), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		initEvent(event);

		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentById(R.id.content) == null) {
			Fragment f = EventDetailsFragment.newInstance(event);
			fm.beginTransaction().add(R.id.content, f).commitAllowingStateLoss();
		}
	}

	/**
	 * Initialize event-related configuration after the event has been loaded.
	 */
	private void initEvent(@NonNull Event event) {
		this.event = event;
		// Enable up navigation only after getting the event details
		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
		toolbar.setNavigationContentDescription(R.string.abc_action_bar_up_description);
		toolbar.setNavigationOnClickListener(v -> onSupportNavigateUp());
		toolbar.setTitle(event.getTrack().getName());

		final Track.Type trackType = event.getTrack().getType();
		if (ThemeUtils.isLightTheme(this)) {
			final ColorStateList trackAppBarColor = ContextCompat.getColorStateList(this, trackType.getAppBarColorResId());
			final int trackStatusBarColor = ContextCompat.getColor(this, trackType.getStatusBarColorResId());
			ThemeUtils.setActivityColors(this, trackAppBarColor.getDefaultColor(), trackStatusBarColor);
			ThemeUtils.tintBackground(appBarLayout, trackAppBarColor);
		} else {
			final ColorStateList trackTextColor = ContextCompat.getColorStateList(this, trackType.getTextColorResId());
			toolbar.setTitleTextColor(trackTextColor);
		}

		bookmarkStatusViewModel.setEvent(event);

		// Enable Android Beam
		NfcUtils.setAppDataPushMessageCallbackIfAvailable(this, this);
	}

	@Nullable
	@Override
	public Intent getSupportParentActivityIntent() {
		// Navigate up to the track associated with this event
		return new Intent(this, TrackScheduleActivity.class)
				.putExtra(TrackScheduleActivity.EXTRA_DAY, event.getDay())
				.putExtra(TrackScheduleActivity.EXTRA_TRACK, event.getTrack())
				.putExtra(TrackScheduleActivity.EXTRA_FROM_EVENT_ID, event.getId());
	}

	@Override
	public void supportNavigateUpTo(@NonNull Intent upIntent) {
		// Replicate the compatibility implementation of NavUtils.navigateUpTo()
		// to ensure the parent Activity is always launched
		// even if not present on the back stack.
		upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(upIntent);
		finish();
	}

	// CreateNfcAppDataCallback

	@Override
	public NdefRecord createNfcAppData() {
		return NfcUtils.createEventAppData(this, event);
	}
}
