package be.digitalia.fosdem.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NdefRecord;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.api.FosdemUrls;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.fragments.BookmarksListFragment;
import be.digitalia.fosdem.fragments.LiveFragment;
import be.digitalia.fosdem.fragments.MapFragment;
import be.digitalia.fosdem.fragments.PersonsListFragment;
import be.digitalia.fosdem.fragments.TracksFragment;
import be.digitalia.fosdem.livedata.SingleEvent;
import be.digitalia.fosdem.model.DownloadScheduleResult;
import be.digitalia.fosdem.utils.CustomTabsUtils;
import be.digitalia.fosdem.utils.NfcUtils;

/**
 * Main entry point of the application. Allows to switch between section fragments and update the database.
 *
 * @author Christophe Beyls
 */
public class MainActivity extends AppCompatActivity implements NfcUtils.CreateNfcAppDataCallback {

	public static final String ACTION_SHORTCUT_BOOKMARKS = BuildConfig.APPLICATION_ID + ".intent.action.SHORTCUT_BOOKMARKS";
	public static final String ACTION_SHORTCUT_LIVE = BuildConfig.APPLICATION_ID + ".intent.action.SHORTCUT_LIVE";

	private enum Section {
		TRACKS(R.id.menu_tracks, true, true) {
			@Override
			public Fragment createFragment() {
				return new TracksFragment();
			}
		},
		BOOKMARKS(R.id.menu_bookmarks, false, false) {
			@Override
			public Fragment createFragment() {
				return new BookmarksListFragment();
			}
		},
		LIVE(R.id.menu_live, true, false) {
			@Override
			public Fragment createFragment() {
				return new LiveFragment();
			}
		},
		SPEAKERS(R.id.menu_speakers, false, false) {
			@Override
			public Fragment createFragment() {
				return new PersonsListFragment();
			}
		},
		MAP(R.id.menu_map, false, false) {
			@Override
			public Fragment createFragment() {
				return new MapFragment();
			}
		};

		private final int menuItemId;
		private final boolean extendsAppBar;
		private final boolean keep;

		Section(@IdRes int menuItemId, boolean extendsAppBar, boolean keep) {
			this.menuItemId = menuItemId;
			this.extendsAppBar = extendsAppBar;
			this.keep = keep;
		}

		@IdRes
		public int getMenuItemId() {
			return menuItemId;
		}

		public abstract Fragment createFragment();

		public boolean extendsAppBar() {
			return extendsAppBar;
		}

		public boolean shouldKeep() {
			return keep;
		}

		@Nullable
		public static Section fromMenuItemId(@IdRes int menuItemId) {
			for (Section section : Section.values()) {
				if (section.menuItemId == menuItemId) {
					return section;
				}
			}
			return null;
		}
	}

	private static final int ERROR_MESSAGE_DISPLAY_DURATION = 5000;
	private static final long DATABASE_VALIDITY_DURATION = DateUtils.DAY_IN_MILLIS;
	private static final long AUTO_UPDATE_SNOOZE_DURATION = DateUtils.DAY_IN_MILLIS;
	private static final String PREF_LAST_AUTO_UPDATE_TIME = "last_download_reminder_time";

	private static final String LAST_UPDATE_DATE_FORMAT = "d MMM yyyy kk:mm:ss";


	private View contentView;

	// Main menu
	Section currentSection;
	MenuItem pendingNavigationMenuItem = null;
	DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private NavigationView navigationView;
	private TextView lastUpdateTextView;

	private MenuItem searchMenuItem;

	@SuppressLint("WrongConstant")
	private final Observer<SingleEvent<DownloadScheduleResult>> scheduleDownloadResultObserver = singleEvent -> {
		final DownloadScheduleResult result = singleEvent.consume();
		if (result == null) {
			return;
		}
		final Snackbar snackbar;
		if (result.isError()) {
			snackbar = Snackbar.make(contentView, R.string.schedule_loading_error, ERROR_MESSAGE_DISPLAY_DURATION)
					.setAction(R.string.schedule_loading_retry_action, v -> FosdemApi.downloadSchedule(this));
		} else if (result.isUpToDate()) {
			snackbar = Snackbar.make(contentView, R.string.events_download_up_to_date, Snackbar.LENGTH_LONG);
		} else {
			final int eventsCount = result.getEventsCount();
			final String message;
			if (eventsCount == 0) {
				message = getString(R.string.events_download_empty);
			} else {
				message = getResources().getQuantityString(R.plurals.events_download_completed, eventsCount, eventsCount);
			}
			snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_LONG);
		}
		snackbar.show();
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		setSupportActionBar(findViewById(R.id.toolbar));
		contentView = findViewById(R.id.content);

		// Progress bar setup
		final ProgressBar progressBar = findViewById(R.id.progress);
		FosdemApi.getDownloadScheduleProgress().observe(this, progressInteger -> {
			int progress = progressInteger;
			if (progress != 100) {
				// Visible
				if (progressBar.getVisibility() == View.GONE) {
					progressBar.clearAnimation();
					progressBar.setVisibility(View.VISIBLE);
				}
				if (progress == -1) {
					progressBar.setIndeterminate(true);
				} else {
					progressBar.setIndeterminate(false);
					progressBar.setProgress(progress);
				}
			} else {
				// Invisible
				if (progressBar.getVisibility() == View.VISIBLE) {
					// Hide the progress bar with a fill and fade out animation
					progressBar.setIndeterminate(false);
					progressBar.setProgress(100);
					progressBar.animate()
							.alpha(0f)
							.withLayer()
							.setListener(new AnimatorListenerAdapter() {
								@Override
								public void onAnimationEnd(Animator animation) {
									progressBar.setVisibility(View.GONE);
									progressBar.setAlpha(1f);
								}
							});
				}
			}
		});

		// Monitor the schedule download result
		FosdemApi.getDownloadScheduleResult().observe(this, scheduleDownloadResultObserver);

		// Setup drawer layout
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		drawerLayout = findViewById(R.id.drawer_layout);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.main_menu, R.string.close_menu) {

			@Override
			public void onDrawerStateChanged(int newState) {
				super.onDrawerStateChanged(newState);
				if (newState == DrawerLayout.STATE_DRAGGING) {
					pendingNavigationMenuItem = null;
				}
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				// Make keypad navigation easier
				navigationView.requestFocus();
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				if (pendingNavigationMenuItem != null) {
					handleNavigationMenuItem(pendingNavigationMenuItem);
					pendingNavigationMenuItem = null;
				}
			}
		};
		drawerToggle.setDrawerIndicatorEnabled(true);
		drawerLayout.addDrawerListener(drawerToggle);
		// Disable drawerLayout focus to allow trackball navigation.
		// We handle the drawer closing on back press ourselves.
		drawerLayout.setFocusable(false);

		// Setup Main menu
		navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(menuItem -> {
			pendingNavigationMenuItem = menuItem;
			drawerLayout.closeDrawer(navigationView);
			return true;
		});

		// Last update date, below the list
		lastUpdateTextView = navigationView.findViewById(R.id.last_update);
		AppDatabase.getInstance(this).getScheduleDao().getLastUpdateTime()
				.observe(this, lastUpdateTimeObserver);

		if (savedInstanceState == null) {
			// Select initial section
			currentSection = Section.TRACKS;
			String action = getIntent().getAction();
			if (action != null) {
				switch (action) {
					case ACTION_SHORTCUT_BOOKMARKS:
						currentSection = Section.BOOKMARKS;
						break;
					case ACTION_SHORTCUT_LIVE:
						currentSection = Section.LIVE;
						break;
				}
			}
			navigationView.setCheckedItem(currentSection.getMenuItemId());

			getSupportFragmentManager().beginTransaction()
					.add(R.id.content, currentSection.createFragment(), currentSection.name())
					.commit();
		}

		NfcUtils.setAppDataPushMessageCallbackIfAvailable(this, this);
	}

	@SuppressLint("PrivateResource")
	private void updateActionBar(@NonNull Section section, @NonNull MenuItem menuItem) {
		setTitle(menuItem.getTitle());
		ViewCompat.setTranslationZ(contentView, section.extendsAppBar()
				? getResources().getDimension(R.dimen.design_appbar_elevation) : 0f);
	}

	private final Observer<Long> lastUpdateTimeObserver = new Observer<Long>() {
		@Override
		public void onChanged(Long lastUpdateTime) {
			lastUpdateTextView.setText(getString(R.string.last_update,
					(lastUpdateTime == -1L)
							? getString(R.string.never)
							: android.text.format.DateFormat.format(LAST_UPDATE_DATE_FORMAT, lastUpdateTime)));
		}
	};

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();

		// Restore current section from NavigationView
		final MenuItem menuItem = navigationView.getCheckedItem();
		if (menuItem != null) {
			if (currentSection == null) {
				currentSection = Section.fromMenuItemId(menuItem.getItemId());
			}
			if (currentSection != null) {
				updateActionBar(currentSection, menuItem);
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (drawerLayout.isDrawerOpen(navigationView)) {
			drawerLayout.closeDrawer(navigationView);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		// Ensure no fragment transaction attempt will occur after onSaveInstanceState()
		if (pendingNavigationMenuItem != null) {
			pendingNavigationMenuItem = null;
			if (currentSection != null) {
				navigationView.setCheckedItem(currentSection.getMenuItemId());
			}
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			if (getDelegate().applyDayNight()) {
				recreate();
			}
		}
		super.onStart();

		// Scheduled database update
		final long now = System.currentTimeMillis();
		final Long timeValue = AppDatabase.getInstance(this).getScheduleDao().getLastUpdateTime().getValue();
		long time = (timeValue == null) ? -1L : timeValue;
		if ((time == -1L) || (time < (now - DATABASE_VALIDITY_DURATION))) {
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			time = prefs.getLong(PREF_LAST_AUTO_UPDATE_TIME, -1L);
			if ((time == -1L) || (time < (now - AUTO_UPDATE_SNOOZE_DURATION))) {
				prefs.edit()
						.putLong(PREF_LAST_AUTO_UPDATE_TIME, now)
						.apply();

				// Try to update immediately. If it fails, the user gets a message and a retry button.
				FosdemApi.downloadSchedule(this);
			}
		}
	}

	@Override
	protected void onStop() {
		if ((searchMenuItem != null) && searchMenuItem.isActionViewExpanded()) {
			searchMenuItem.collapseActionView();
		}

		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem searchMenuItem = menu.findItem(R.id.search);
		this.searchMenuItem = searchMenuItem;
		searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				// Workaround for disappearing menu items bug
				supportInvalidateOptionsMenu();
				return true;
			}
		});
		// Associate searchable configuration with the SearchView
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) searchMenuItem.getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		// Will close the drawer if the home button is pressed
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.refresh:
				Drawable icon = item.getIcon();
				if (icon instanceof Animatable) {
					// Hack: reset the icon to make sure the MenuItem will redraw itself properly
					item.setIcon(icon);
					((Animatable) icon).start();
				}
				FosdemApi.downloadSchedule(this);
				return true;
		}
		return false;
	}

	// MAIN MENU

	void handleNavigationMenuItem(@NonNull MenuItem menuItem) {
		final int menuItemId = menuItem.getItemId();
		final Section section = Section.fromMenuItemId(menuItemId);
		if (section != null) {
			selectMenuSection(section, menuItem);
		} else {
			switch (menuItemId) {
				case R.id.menu_settings:
					startActivity(new Intent(MainActivity.this, SettingsActivity.class));
					overridePendingTransition(R.anim.slide_in_right, R.anim.partial_zoom_out);
					break;
				case R.id.menu_volunteer:
					try {
						CustomTabsUtils.configureToolbarColors(new CustomTabsIntent.Builder(), this, R.color.light_color_primary)
								.setShowTitle(true)
								.build()
								.launchUrl(this, Uri.parse(FosdemUrls.getVolunteer()));
					} catch (ActivityNotFoundException ignore) {
					}
					break;
			}
		}
	}

	void selectMenuSection(@NonNull Section section, @NonNull MenuItem menuItem) {
		if (section != currentSection) {
			// Switch to new section
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment f = fm.findFragmentById(R.id.content);
			if (f != null) {
				if (currentSection.shouldKeep()) {
					ft.detach(f);
				} else {
					ft.remove(f);
				}
			}
			if (section.shouldKeep() && ((f = fm.findFragmentByTag(section.name())) != null)) {
				ft.attach(f);
			} else {
				ft.add(R.id.content, section.createFragment(), section.name());
			}
			ft.commit();

			currentSection = section;
			updateActionBar(section, menuItem);
		}
	}

	@Nullable
	@Override
	public NdefRecord createNfcAppData() {
		// Delegate to the currently displayed fragment if it provides NFC data
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.content);
		if (f instanceof NfcUtils.CreateNfcAppDataCallback) {
			return ((NfcUtils.CreateNfcAppDataCallback) f).createNfcAppData();
		}
		return null;
	}
}
