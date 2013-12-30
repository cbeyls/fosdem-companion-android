package be.digitalia.fosdem.activities;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.fragments.BookmarksListFragment;
import be.digitalia.fosdem.fragments.MapFragment;
import be.digitalia.fosdem.fragments.MessageDialogFragment;
import be.digitalia.fosdem.fragments.PersonsListFragment;
import be.digitalia.fosdem.fragments.TracksFragment;
import be.digitalia.fosdem.loaders.LocalCacheLoader;

public class MainActivity extends ActionBarActivity implements ListView.OnItemClickListener, Handler.Callback {

	private enum Section {
		TRACKS(TracksFragment.class, R.string.menu_tracks, R.drawable.ic_action_event, true), BOOKMARKS(BookmarksListFragment.class, R.string.menu_bookmarks,
				R.drawable.ic_action_important, false), SPEAKERS(PersonsListFragment.class, R.string.menu_speakers, R.drawable.ic_action_group, false), MAP(
				MapFragment.class, R.string.menu_map, R.drawable.ic_action_map, false);

		private final String fragmentClassName;
		private final int titleResId;
		private final int iconResId;
		private final boolean keep;

		private Section(Class<? extends Fragment> fragmentClass, int titleResId, int iconResId, boolean keep) {
			this.fragmentClassName = fragmentClass.getName();
			this.titleResId = titleResId;
			this.iconResId = iconResId;
			this.keep = keep;
		}

		public String getFragmentClassName() {
			return fragmentClassName;
		}

		public int getTitleResId() {
			return titleResId;
		}

		public int getIconResId() {
			return iconResId;
		}

		public boolean shouldKeep() {
			return keep;
		}
	}

	private static final int DOWNLOAD_SCHEDULE_LOADER_ID = 1;
	private static final int DOWNLOAD_SCHEDULE_RESULT_WHAT = 1;

	private static final long DATABASE_VALIDITY_DURATION = 24L * 60L * 60L * 1000L; // 24h
	private static final long DOWNLOAD_REMINDER_SNOOZE_DURATION = 24L * 60L * 60L * 1000L; // 24h
	private static final String PREF_LAST_DOWNLOAD_REMINDER_TIME = "last_download_reminder_time";
	private static final String STATE_CURRENT_SECTION = "current_section";

	private static final DateFormat LAST_UPDATE_DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());

	private Handler handler;

	private Section currentSection;

	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle drawerToggle;
	private View mainMenu;
	private TextView lastUpdateTextView;
	private MainMenuAdapter menuAdapter;

	private final BroadcastReceiver scheduleLoadingProgressReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			setSupportProgressBarIndeterminate(false);
			setSupportProgress(intent.getIntExtra(FosdemApi.EXTRA_PROGRESS, 0) * 100);
		}
	};

	private final BroadcastReceiver scheduleRefreshedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateLastUpdateTime();
		}
	};

	public static class DownloadScheduleReminderDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getActivity()).setTitle(R.string.download_reminder_title).setMessage(R.string.download_reminder_message)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							((MainActivity) getActivity()).startDownloadSchedule();
						}

					}).setNegativeButton(android.R.string.cancel, null).create();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_PROGRESS);
		handler = new Handler(this);
		setContentView(R.layout.main);

		// Connect the main progress bar
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.registerReceiver(scheduleLoadingProgressReceiver, new IntentFilter(FosdemApi.ACTION_SCHEDULE_PROGRESS));

		// Setup drawer layout
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerLayout.setDrawerShadow(getResources().getDrawable(R.drawable.drawer_shadow), Gravity.LEFT);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.main_menu, R.string.close_menu) {

			@Override
			public void onDrawerOpened(View drawerView) {
				updateActionBar();
				supportInvalidateOptionsMenu();
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				updateActionBar();
				supportInvalidateOptionsMenu();
			}
		};
		drawerToggle.setDrawerIndicatorEnabled(true);
		drawerLayout.setDrawerListener(drawerToggle);
		// Disable drawerLayout focus to allow trackball navigation.
		// We handle the drawer closing on back press ourselves.
		drawerLayout.setFocusable(false);

		// Setup Main menu
		mainMenu = findViewById(R.id.main_menu);
		ListView menuListView = (ListView) findViewById(R.id.main_menu_list);
		LayoutInflater inflater = LayoutInflater.from(this);
		View menuHeaderView = inflater.inflate(R.layout.header_main_menu, null);
		menuListView.addHeaderView(menuHeaderView, null, false);
		lbm.registerReceiver(scheduleRefreshedReceiver, new IntentFilter(DatabaseManager.ACTION_SCHEDULE_REFRESHED));
		menuAdapter = new MainMenuAdapter(inflater);
		menuListView.setAdapter(menuAdapter);
		menuListView.setOnItemClickListener(this);

		// Last update date, below the menu
		lastUpdateTextView = (TextView) findViewById(R.id.last_update);
		updateLastUpdateTime();

		// Restore current section
		if (savedInstanceState == null) {
			currentSection = Section.TRACKS;
			Fragment f = Fragment.instantiate(this, currentSection.getFragmentClassName());
			getSupportFragmentManager().beginTransaction().add(R.id.content, f).commit();
		} else {
			currentSection = Section.values()[savedInstanceState.getInt(STATE_CURRENT_SECTION)];
		}
		// Ensure the current section is visible in the menu
		menuListView.setSelection(currentSection.ordinal());
		updateActionBar();

		// Loader
		if (getSupportLoaderManager().getLoader(DOWNLOAD_SCHEDULE_LOADER_ID) != null) {
			// Reconnect to running loader
			startDownloadSchedule();
		}
	}

	private void updateActionBar() {
		getSupportActionBar().setTitle(drawerLayout.isDrawerOpen(mainMenu) ? R.string.app_name : currentSection.getTitleResId());
	}

	private void updateLastUpdateTime() {
		long lastUpdateTime = DatabaseManager.getInstance().getLastUpdateTime();
		lastUpdateTextView.setText(getString(R.string.last_update,
				(lastUpdateTime == -1L) ? getString(R.string.never) : LAST_UPDATE_DATE_FORMAT.format(new Date(lastUpdateTime))));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		if (drawerLayout.isDrawerOpen(mainMenu)) {
			updateActionBar();
		}
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
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CURRENT_SECTION, currentSection.ordinal());
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
				prefs.edit().putLong(PREF_LAST_DOWNLOAD_REMINDER_TIME, now).commit();

				FragmentManager fm = getSupportFragmentManager();
				if (fm.findFragmentByTag("download_reminder") == null) {
					new DownloadScheduleReminderDialogFragment().show(fm, "download_reminder");
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
		lbm.unregisterReceiver(scheduleLoadingProgressReceiver);
		lbm.unregisterReceiver(scheduleRefreshedReceiver);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	};

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Hide & disable primary (contextual) action items when the main menu is opened
		if (drawerLayout.isDrawerOpen(mainMenu)) {
			final int size = menu.size();
			for (int i = 0; i < size; ++i) {
				MenuItem item = menu.getItem(i);
				if ((item.getOrder() & 0xFFFF0000) == 0) {
					item.setVisible(false).setEnabled(false);
				}
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Will close the drawer if the home button is pressed
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
		case R.id.search:
			onSearchRequested();
			return true;
		case R.id.refresh:
			startDownloadSchedule();
			return true;
		}
		return false;
	}

	public void startDownloadSchedule() {
		// Start by displaying indeterminate progress, determinate will come later
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarVisibility(true);
		getSupportLoaderManager().initLoader(DOWNLOAD_SCHEDULE_LOADER_ID, null, downloadScheduleLoaderCallbacks);
	}

	private static class DownloadScheduleLoader extends LocalCacheLoader<Integer> {

		public DownloadScheduleLoader(Context context) {
			super(context);
		}

		@Override
		public Integer loadInBackground() {
			return FosdemApi.downloadSchedule(getContext());
		}
	}

	private final LoaderCallbacks<Integer> downloadScheduleLoaderCallbacks = new LoaderCallbacks<Integer>() {

		@Override
		public Loader<Integer> onCreateLoader(int id, Bundle args) {
			return new DownloadScheduleLoader(MainActivity.this);
		}

		@Override
		public void onLoadFinished(Loader<Integer> loader, Integer data) {
			handler.sendMessage(handler.obtainMessage(DOWNLOAD_SCHEDULE_RESULT_WHAT, data, 0));
		}

		@Override
		public void onLoaderReset(Loader<Integer> loader) {
		}
	};

	@Override
	public boolean handleMessage(Message message) {
		switch (message.what) {
		case DOWNLOAD_SCHEDULE_RESULT_WHAT:
			getSupportLoaderManager().destroyLoader(DOWNLOAD_SCHEDULE_LOADER_ID);
			int result = message.arg1;
			if (result == FosdemApi.RESULT_ERROR) {
				MessageDialogFragment.newInstance(R.string.error_title, R.string.schedule_loading_error).show(getSupportFragmentManager(), "error");
			} else {
				Toast.makeText(this, getString(R.string.events_download_completed, result), Toast.LENGTH_LONG).show();
			}
			return true;
		}
		return false;
	}

	// MAIN MENU

	private class MainMenuAdapter extends BaseAdapter {

		private Section[] sections = Section.values();
		private LayoutInflater inflater;
		private int currentSectionBackgroundColor;

		public MainMenuAdapter(LayoutInflater inflater) {
			this.inflater = inflater;
			currentSectionBackgroundColor = getResources().getColor(R.color.translucent_grey);
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
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_main_menu, parent, false);
			}

			Section section = getItem(position);

			TextView tv = (TextView) convertView.findViewById(R.id.section_text);
			tv.setText(section.getTitleResId());
			tv.setCompoundDrawablesWithIntrinsicBounds(section.getIconResId(), 0, 0, 0);
			// Show highlighted background for current section
			tv.setBackgroundColor((section == currentSection) ? currentSectionBackgroundColor : Color.TRANSPARENT);

			return convertView;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// Decrease position by 1 since the listView has a header view.
		Section section = menuAdapter.getItem(position - 1);
		if (section != currentSection) {
			// Switch to new section
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
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
				f = Fragment.instantiate(this, fragmentClassName);
				ft.add(R.id.content, f, fragmentClassName);
			}
			ft.commit();

			currentSection = section;
			menuAdapter.notifyDataSetChanged();
		}

		drawerLayout.closeDrawer(mainMenu);
	}
}
