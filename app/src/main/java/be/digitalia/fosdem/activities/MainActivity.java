package be.digitalia.fosdem.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import be.digitalia.fosdem.BuildConfig;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.fragments.BookmarksListFragment;
import be.digitalia.fosdem.fragments.LiveFragment;
import be.digitalia.fosdem.fragments.MapFragment;
import be.digitalia.fosdem.fragments.PersonsListFragment;
import be.digitalia.fosdem.fragments.TracksFragment;
import be.digitalia.fosdem.widgets.AdapterLinearLayout;

/**
 * Main entry point of the application. Allows to switch between section fragments and update the database.
 *
 * @author Christophe Beyls
 */
public class MainActivity extends AppCompatActivity implements Handler.Callback {

	private enum Section {
		TRACKS(TracksFragment.class, R.string.menu_tracks, R.drawable.ic_event_grey600_24dp, true, true),
		BOOKMARKS(BookmarksListFragment.class, R.string.menu_bookmarks, R.drawable.ic_bookmark_grey600_24dp, false, false),
		LIVE(LiveFragment.class, R.string.menu_live, R.drawable.ic_play_circle_outline_grey600_24dp, true, false),
		SPEAKERS(PersonsListFragment.class, R.string.menu_speakers, R.drawable.ic_people_grey600_24dp, false, false),
		MAP(MapFragment.class, R.string.menu_map, R.drawable.ic_map_grey600_24dp, false, false);

		private final String fragmentClassName;
		private final int titleResId;
		private final int iconResId;
		private final boolean extendsAppBar;
		private final boolean keep;

		Section(Class<? extends Fragment> fragmentClass, int titleResId, int iconResId,
				boolean extendsAppBar, boolean keep) {
			this.fragmentClassName = fragmentClass.getName();
			this.titleResId = titleResId;
			this.iconResId = iconResId;
			this.extendsAppBar = extendsAppBar;
			this.keep = keep;
		}

		public String getFragmentClassName() {
			return fragmentClassName;
		}

		@StringRes
		public int getTitleResId() {
			return titleResId;
		}

		@DrawableRes
		public int getIconResId() {
			return iconResId;
		}

		public boolean extendsAppBar() {
			return extendsAppBar;
		}

		public boolean shouldKeep() {
			return keep;
		}
	}

	private static final int SELECT_MENU_SECTION_WHAT = 1;
	private static final int SELECT_MENU_FOOTER_WHAT = 2;

	private static final long MENU_ACTION_DELAY = 400L;

	private static final long DATABASE_VALIDITY_DURATION = DateUtils.DAY_IN_MILLIS;
	private static final long DOWNLOAD_REMINDER_SNOOZE_DURATION = DateUtils.DAY_IN_MILLIS;
	private static final String PREF_LAST_DOWNLOAD_REMINDER_TIME = "last_download_reminder_time";
	private static final String STATE_CURRENT_SECTION = "current_section";

	private static final String LAST_UPDATE_DATE_FORMAT = "d MMM yyyy kk:mm:ss";


	private Handler handler;
	private Toolbar toolbar;
	private ProgressBar progressBar;

	// Main menu
	private Section currentSection;
	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private View mainMenu;
	private TextView lastUpdateTextView;
	private MainMenuAdapter menuAdapter;

	private MenuItem searchMenuItem;

	private final BroadcastReceiver scheduleDownloadProgressReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			progressBar.setIndeterminate(false);
			progressBar.setProgress(intent.getIntExtra(FosdemApi.EXTRA_PROGRESS, 0));
		}
	};

	private final BroadcastReceiver scheduleDownloadResultReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Hide the progress bar with a fill and fade out animation
			progressBar.setIndeterminate(false);
			progressBar.setProgress(100);
			progressBar.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.fade_out));
			progressBar.setVisibility(View.GONE);

			int result = intent.getIntExtra(FosdemApi.EXTRA_RESULT, FosdemApi.RESULT_ERROR);
			String message;
			switch (result) {
				case FosdemApi.RESULT_ERROR:
					message = getString(R.string.schedule_loading_error);
					break;
				case FosdemApi.RESULT_UP_TO_DATE:
					message = getString(R.string.events_download_up_to_date);
					break;
				case 0:
					message = getString(R.string.events_download_empty);
					break;
				default:
					message = getResources().getQuantityString(R.plurals.events_download_completed, result, result);
			}
			Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
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
			return new AlertDialog.Builder(getActivity())
					.setTitle(R.string.download_reminder_title)
					.setMessage(R.string.download_reminder_message)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							((MainActivity) getActivity()).startDownloadSchedule();
						}

					}).setNegativeButton(android.R.string.cancel, null)
					.create();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		handler = new Handler(this);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		progressBar = (ProgressBar) findViewById(R.id.progress);

		// Setup drawer layout
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerLayout.setDrawerShadow(getResources().getDrawable(R.drawable.drawer_shadow), Gravity.LEFT);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.main_menu, R.string.close_menu) {

			@Override
			public void onDrawerOpened(View drawerView) {
				// Make keypad navigation easier
				mainMenu.requestFocus();
			}
		};
		drawerToggle.setDrawerIndicatorEnabled(true);
		drawerLayout.setDrawerListener(drawerToggle);
		// Disable drawerLayout focus to allow trackball navigation.
		// We handle the drawer closing on back press ourselves.
		drawerLayout.setFocusable(false);

		// Setup Main menu
		mainMenu = findViewById(R.id.main_menu);
		final AdapterLinearLayout sectionsList = (AdapterLinearLayout) findViewById(R.id.sections);
		menuAdapter = new MainMenuAdapter(getLayoutInflater());
		sectionsList.setAdapter(menuAdapter);
		mainMenu.findViewById(R.id.settings).setOnClickListener(menuFooterClickListener);
		mainMenu.findViewById(R.id.about).setOnClickListener(menuFooterClickListener);

		LocalBroadcastManager.getInstance(this).registerReceiver(scheduleRefreshedReceiver, new IntentFilter(DatabaseManager.ACTION_SCHEDULE_REFRESHED));

		// Last update date, below the list
		lastUpdateTextView = (TextView) mainMenu.findViewById(R.id.last_update);
		updateLastUpdateTime();

		// Restore current section
		if (savedInstanceState == null) {
			currentSection = Section.TRACKS;
			String fragmentClassName = currentSection.getFragmentClassName();
			Fragment f = Fragment.instantiate(this, fragmentClassName);
			getSupportFragmentManager().beginTransaction().add(R.id.content, f, fragmentClassName).commit();
		} else {
			currentSection = Section.values()[savedInstanceState.getInt(STATE_CURRENT_SECTION)];
		}
		// Ensure the current section is visible in the menu
		sectionsList.post(new Runnable() {
			@Override
			public void run() {
				if (sectionsList.getChildCount() > currentSection.ordinal()) {
					ScrollView mainMenuScrollView = (ScrollView) findViewById(R.id.main_menu_scroll);
					int requiredScroll = sectionsList.getTop()
							+ sectionsList.getChildAt(currentSection.ordinal()).getBottom()
							- mainMenuScrollView.getHeight();
					mainMenuScrollView.scrollTo(0, Math.max(0, requiredScroll));
				}
			}
		});
		updateActionBar();
	}

	private void updateActionBar() {
		getSupportActionBar().setTitle(currentSection.getTitleResId());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			toolbar.setElevation(currentSection.extendsAppBar()
					? 0f : getResources().getDimension(R.dimen.toolbar_elevation));
		}
	}

	private void updateLastUpdateTime() {
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
		handler.removeCallbacksAndMessages(null);
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CURRENT_SECTION, currentSection.ordinal());
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Ensure the progress bar is hidden when starting
		progressBar.setVisibility(View.GONE);

		// Monitor the schedule download
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.registerReceiver(scheduleDownloadProgressReceiver, new IntentFilter(FosdemApi.ACTION_DOWNLOAD_SCHEDULE_PROGRESS));
		lbm.registerReceiver(scheduleDownloadResultReceiver, new IntentFilter(FosdemApi.ACTION_DOWNLOAD_SCHEDULE_RESULT));

		// Download reminder
		long now = System.currentTimeMillis();
		long time = DatabaseManager.getInstance().getLastUpdateTime();
		if ((time == -1L) || (time < (now - DATABASE_VALIDITY_DURATION))) {
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			time = prefs.getLong(PREF_LAST_DOWNLOAD_REMINDER_TIME, -1L);
			if ((time == -1L) || (time < (now - DOWNLOAD_REMINDER_SNOOZE_DURATION))) {
				prefs.edit().putLong(PREF_LAST_DOWNLOAD_REMINDER_TIME, now).commit();

				FragmentManager fm = getSupportFragmentManager();
				if (fm.findFragmentByTag("download_reminder") == null) {
					new DownloadScheduleReminderDialogFragment().show(fm, "download_reminder");
				}
			}
		}
	}

	@Override
	protected void onStop() {
		if ((searchMenuItem != null) && (MenuItemCompat.isActionViewExpanded(searchMenuItem))) {
			MenuItemCompat.collapseActionView(searchMenuItem);
		}

		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.unregisterReceiver(scheduleDownloadProgressReceiver);
		lbm.unregisterReceiver(scheduleDownloadResultReceiver);

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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			this.searchMenuItem = searchMenuItem;
			// Associate searchable configuration with the SearchView
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
			SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		} else {
			// Legacy search mode for Eclair
			MenuItemCompat.setActionView(searchMenuItem, null);
			MenuItemCompat.setShowAsAction(searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Will close the drawer if the home button is pressed
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.search:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
					return false;
				} else {
					// Legacy search mode for Eclair
					onSearchRequested();
					return true;
				}
			case R.id.refresh:
				startDownloadSchedule();
				return true;
		}
		return false;
	}

	@SuppressLint("NewApi")
	public void startDownloadSchedule() {
		// Start by displaying indeterminate progress, determinate will come later
		progressBar.clearAnimation();
		progressBar.setIndeterminate(true);
		progressBar.setVisibility(View.VISIBLE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			new DownloadScheduleAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			new DownloadScheduleAsyncTask(this).execute();
		}
	}

	private static class DownloadScheduleAsyncTask extends AsyncTask<Void, Void, Void> {

		private final Context appContext;

		public DownloadScheduleAsyncTask(Context context) {
			appContext = context.getApplicationContext();
		}

		@Override
		protected Void doInBackground(Void... args) {
			FosdemApi.downloadSchedule(appContext);
			return null;
		}
	}

	// MAIN MENU

	private class MainMenuAdapter extends AdapterLinearLayout.Adapter<Section> {

		private final Section[] sections = Section.values();
		private final LayoutInflater inflater;
		private final int currentSectionForegroundColor;

		public MainMenuAdapter(LayoutInflater inflater) {
			this.inflater = inflater;
			// Select the primary color to tint the current section
			TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.colorPrimary});
			try {
				currentSectionForegroundColor = a.getColor(0, Color.TRANSPARENT);
			} finally {
				a.recycle();
			}
		}

		@Override
		public int getCount() {
			return sections.length;
		}

		@Override
		public Section getItem(int position) {
			return sections[position];
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_main_menu, parent, false);
				convertView.setOnClickListener(sectionClickListener);
			}

			Section section = getItem(position);
			convertView.setSelected(section == currentSection);

			TextView tv = (TextView) convertView.findViewById(R.id.section_text);
			SpannableString sectionTitle = new SpannableString(getString(section.getTitleResId()));
			Drawable sectionIcon = getResources().getDrawable(section.getIconResId());
			if (section == currentSection) {
				// Special color for the current section
				sectionTitle.setSpan(new ForegroundColorSpan(currentSectionForegroundColor), 0, sectionTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				// We need to mutate the drawable before applying the ColorFilter, or else all the similar drawable instances will be tinted.
				sectionIcon.mutate().setColorFilter(currentSectionForegroundColor, PorterDuff.Mode.SRC_IN);
			}
			tv.setText(sectionTitle);
			TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(tv, sectionIcon, null, null, null);

			return convertView;
		}
	}

	private final View.OnClickListener sectionClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			int sectionIndex = ((ViewGroup) view.getParent()).indexOfChild(view);
			// Cancel pending section selection, if any
			handler.removeMessages(SELECT_MENU_SECTION_WHAT);
			handler.sendMessageDelayed(handler.obtainMessage(SELECT_MENU_SECTION_WHAT, sectionIndex, 0), MENU_ACTION_DELAY);
			drawerLayout.closeDrawer(mainMenu);
		}
	};

	private void selectMenuSection(int position) {
		Section section = menuAdapter.getItem(position);
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
			updateActionBar();
			menuAdapter.notifyDataSetChanged();
		}
	}

	private final View.OnClickListener menuFooterClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View view) {
			handler.sendMessageDelayed(handler.obtainMessage(SELECT_MENU_FOOTER_WHAT, view.getId(), 0), MENU_ACTION_DELAY);
			drawerLayout.closeDrawer(mainMenu);
		}
	};

	private void selectMenuFooter(int id) {
		switch (id) {
			case R.id.settings:
				startActivity(new Intent(MainActivity.this, SettingsActivity.class));
				overridePendingTransition(R.anim.slide_in_right, R.anim.partial_zoom_out);
				break;
			case R.id.about:
				new AboutDialogFragment().show(getSupportFragmentManager(), "about");
				break;
		}
	}

	@Override
	public boolean handleMessage(Message message) {
		switch (message.what) {
			case SELECT_MENU_SECTION_WHAT:
				selectMenuSection(message.arg1);
				return true;
			case SELECT_MENU_FOOTER_WHAT:
				selectMenuFooter(message.arg1);
				return true;
		}
		return false;
	}

	public static class AboutDialogFragment extends DialogFragment {

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			String title = String.format("%1$s %2$s", getString(R.string.app_name), BuildConfig.VERSION_NAME);
			return new AlertDialog.Builder(getActivity())
					.setTitle(title)
					.setIcon(R.mipmap.ic_launcher)
					.setMessage(getResources().getText(R.string.about_text))
					.setPositiveButton(android.R.string.ok, null)
					.create();
		}

		@Override
		public void onStart() {
			super.onStart();
			// Make links clickable; must be called after the dialog is shown
			((TextView) getDialog().findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
		}
	}
}
