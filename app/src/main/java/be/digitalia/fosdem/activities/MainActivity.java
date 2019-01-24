package be.digitalia.fosdem.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.*;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.api.FosdemUrls;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.fragments.*;
import be.digitalia.fosdem.livedata.SingleEvent;
import be.digitalia.fosdem.model.DownloadScheduleResult;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

/**
 * Main entry point of the application. Allows to switch between section fragments and update the database.
 *
 * @author Christophe Beyls
 */
public class MainActivity extends AppCompatActivity {

	public static final String ACTION_SHORTCUT_BOOKMARKS = BuildConfig.APPLICATION_ID + ".intent.action.SHORTCUT_BOOKMARKS";
	public static final String ACTION_SHORTCUT_LIVE = BuildConfig.APPLICATION_ID + ".intent.action.SHORTCUT_LIVE";

	private enum Section {
		TRACKS(R.id.menu_tracks, TracksFragment.class, true, true),
		BOOKMARKS(R.id.menu_bookmarks, BookmarksListFragment.class, false, false),
		LIVE(R.id.menu_live, LiveFragment.class, true, false),
		SPEAKERS(R.id.menu_speakers, PersonsListFragment.class, false, false),
		MAP(R.id.menu_map, MapFragment.class, false, false);

		private final int menuItemId;
		private final String fragmentClassName;
		private final boolean extendsAppBar;
		private final boolean keep;

		Section(@IdRes int menuItemId, Class<? extends Fragment> fragmentClass, boolean extendsAppBar, boolean keep) {
			this.menuItemId = menuItemId;
			this.fragmentClassName = fragmentClass.getName();
			this.extendsAppBar = extendsAppBar;
			this.keep = keep;
		}

		@IdRes
		public int getMenuItemId() {
			return menuItemId;
		}

		public String getFragmentClassName() {
			return fragmentClassName;
		}

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

	private static final long DATABASE_VALIDITY_DURATION = DateUtils.DAY_IN_MILLIS;
	private static final long DOWNLOAD_REMINDER_SNOOZE_DURATION = DateUtils.DAY_IN_MILLIS;
	private static final String PREF_LAST_DOWNLOAD_REMINDER_TIME = "last_download_reminder_time";

	private static final String LAST_UPDATE_DATE_FORMAT = "d MMM yyyy kk:mm:ss";


	private Toolbar toolbar;

	// Main menu
	Section currentSection;
	MenuItem pendingNavigationMenuItem = null;
	DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	View mainMenu;
	private NavigationView navigationView;
	private TextView lastUpdateTextView;

	private MenuItem searchMenuItem;

	private final Observer<SingleEvent<DownloadScheduleResult>> scheduleDownloadResultObserver = new Observer<SingleEvent<DownloadScheduleResult>>() {

		@Override
		public void onChanged(SingleEvent<DownloadScheduleResult> singleEvent) {
			final DownloadScheduleResult result = singleEvent.consume();
			if (result == null) {
				return;
			}
			String message;
			if (result.isError()) {
				message = getString(R.string.schedule_loading_error);
			} else if (result.isUpToDate()) {
				message = getString(R.string.events_download_up_to_date);
			} else {
				int eventsCount = result.getEventsCount();
				if (eventsCount == 0) {
					message = getString(R.string.events_download_empty);
				} else {
					message = getResources().getQuantityString(R.plurals.events_download_completed, eventsCount, eventsCount);
				}
			}
			Snackbar.make(findViewById(R.id.content), message, Snackbar.LENGTH_LONG).show();
		}
	};

	private final BroadcastReceiver scheduleRefreshedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateLastUpdateTime();
		}
	};

	public static class DownloadScheduleReminderDialogFragment extends DialogFragment {

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getContext())
					.setTitle(R.string.download_reminder_title)
					.setMessage(R.string.download_reminder_message)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							FosdemApi.downloadSchedule(getContext());
						}

					}).setNegativeButton(android.R.string.cancel, null)
					.create();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Progress bar setup
		final ProgressBar progressBar = findViewById(R.id.progress);
		FosdemApi.getDownloadScheduleProgress().observe(this, new Observer<Integer>() {

			@Override
			public void onChanged(Integer progressInteger) {
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
								.setListener(new AnimatorListenerAdapter() {
									@Override
									public void onAnimationEnd(Animator animation) {
										progressBar.setVisibility(View.GONE);
										progressBar.setAlpha(1f);
									}
								});
					}
				}
			}
		});

		// Monitor the schedule download result
		FosdemApi.getDownloadScheduleResult().observe(this, scheduleDownloadResultObserver);

		// Setup drawer layout
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		drawerLayout = findViewById(R.id.drawer_layout);
		drawerLayout.setDrawerShadow(ContextCompat.getDrawable(this, R.drawable.drawer_shadow), GravityCompat.START);
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
				mainMenu.requestFocus();
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
		mainMenu = findViewById(R.id.main_menu);
		// Forward window insets to NavigationView
		ViewCompat.setOnApplyWindowInsetsListener(mainMenu, new OnApplyWindowInsetsListener() {
			@Override
			public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
				return insets;
			}
		});
		navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				pendingNavigationMenuItem = menuItem;
				drawerLayout.closeDrawer(mainMenu);
				return true;
			}
		});

		LocalBroadcastManager.getInstance(this).registerReceiver(scheduleRefreshedReceiver, new IntentFilter(DatabaseManager.ACTION_SCHEDULE_REFRESHED));

		// Last update date, below the list
		lastUpdateTextView = mainMenu.findViewById(R.id.last_update);
		updateLastUpdateTime();

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

			String fragmentClassName = currentSection.getFragmentClassName();
			Fragment f = Fragment.instantiate(this, fragmentClassName);
			getSupportFragmentManager().beginTransaction().add(R.id.content, f, fragmentClassName).commit();
		}
	}

	private void updateActionBar(@NonNull Section section, @NonNull MenuItem menuItem) {
		setTitle(menuItem.getTitle());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			toolbar.setElevation(section.extendsAppBar()
					? 0f : getResources().getDimension(R.dimen.toolbar_elevation));
		}
	}

	void updateLastUpdateTime() {
		long lastUpdateTime = DatabaseManager.getInstance().getLastUpdateTime();
		lastUpdateTextView.setText(getString(R.string.last_update,
				(lastUpdateTime == -1L)
						? getString(R.string.never)
						: android.text.format.DateFormat.format(LAST_UPDATE_DATE_FORMAT, lastUpdateTime)));
	}

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
		if (drawerLayout.isDrawerOpen(mainMenu)) {
			drawerLayout.closeDrawer(mainMenu);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
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
		super.onStart();

		// Download reminder
		long now = System.currentTimeMillis();
		long time = DatabaseManager.getInstance().getLastUpdateTime();
		if ((time == -1L) || (time < (now - DATABASE_VALIDITY_DURATION))) {
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			time = prefs.getLong(PREF_LAST_DOWNLOAD_REMINDER_TIME, -1L);
			if ((time == -1L) || (time < (now - DOWNLOAD_REMINDER_SNOOZE_DURATION))) {
				prefs.edit()
						.putLong(PREF_LAST_DOWNLOAD_REMINDER_TIME, now)
						.apply();

				FragmentManager fm = getSupportFragmentManager();
				if (fm.findFragmentByTag("download_reminder") == null) {
					new DownloadScheduleReminderDialogFragment().show(fm, "download_reminder");
				}
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
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(scheduleRefreshedReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem searchMenuItem = menu.findItem(R.id.search);
		this.searchMenuItem = searchMenuItem;
		// Associate searchable configuration with the SearchView
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) searchMenuItem.getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
						new CustomTabsIntent.Builder()
								.setToolbarColor(ContextCompat.getColor(this, R.color.color_primary))
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
			String fragmentClassName = section.getFragmentClassName();
			if (section.shouldKeep() && ((f = fm.findFragmentByTag(fragmentClassName)) != null)) {
				ft.attach(f);
			} else {
				f = Fragment.instantiate(MainActivity.this, fragmentClassName);
				ft.add(R.id.content, f, fragmentClassName);
			}
			ft.commit();

			currentSection = section;
			updateActionBar(section, menuItem);
		}
	}
}
