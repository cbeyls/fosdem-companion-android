package be.digitalia.fosdem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.EventDetailsFragment;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.utils.NfcUtils;
import be.digitalia.fosdem.utils.NfcUtils.CreateNfcAppDataCallback;
import be.digitalia.fosdem.utils.ThemeUtils;
import be.digitalia.fosdem.viewmodels.EventViewModel;

/**
 * Displays a single event passed either as a complete Parcelable object in extras or as an id in data.
 *
 * @author Christophe Beyls
 */
public class EventDetailsActivity extends AppCompatActivity implements Observer<Event>, CreateNfcAppDataCallback {

	public static final String EXTRA_EVENT = "event";

	private Event event;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(false);
		bar.setDisplayShowTitleEnabled(false);

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
			EventViewModel viewModel = ViewModelProviders.of(this).get(EventViewModel.class);
			if (!viewModel.hasEventId()) {
				Intent intent = getIntent();
				String eventIdString;
				if (NfcUtils.hasAppData(intent)) {
					// NFC intent
					eventIdString = new String(NfcUtils.extractAppData(intent));
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
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		ThemeUtils.setActionBarTrackColor(this, event.getTrack().getType());
		// Enable Android Beam
		NfcUtils.setAppDataPushMessageCallbackIfAvailable(this, this);
	}

	@Override
	public boolean onSupportNavigateUp() {
		// Navigate up to the track associated with this event
		Intent upIntent = new Intent(this, TrackScheduleActivity.class);
		upIntent.putExtra(TrackScheduleActivity.EXTRA_DAY, event.getDay());
		upIntent.putExtra(TrackScheduleActivity.EXTRA_TRACK, event.getTrack());
		upIntent.putExtra(TrackScheduleActivity.EXTRA_FROM_EVENT_ID, event.getId());

		if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
			TaskStackBuilder.create(this)
					.addNextIntentWithParentStack(upIntent)
					.startActivities();
			finish();
		} else {
			// Replicate the compatibility implementation of NavUtils.navigateUpTo()
			// to ensure the parent Activity is always launched
			// even if not present on the back stack.
			upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(upIntent);
			finish();
		}
		return true;
	}

	@Override
	public byte[] createNfcAppData() {
		return String.valueOf(event.getId()).getBytes();
	}
}
