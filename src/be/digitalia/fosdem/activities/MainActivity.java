package be.digitalia.fosdem.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.fragments.MessageDialogFragment;
import be.digitalia.fosdem.loaders.AbstractAsyncTaskLoader;

public class MainActivity extends ActionBarActivity implements Handler.Callback {

	private static final int DOWNLOAD_SCHEDULE_LOADER_ID = 1;

	private static final int DOWNLOAD_SCHEDULE_RESULT_WHAT = 1;

	private Handler handler;

	private final BroadcastReceiver scheduleLoadingProgressReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			setSupportProgressBarIndeterminate(false);
			setSupportProgress(intent.getIntExtra(FosdemApi.PROGRESS_EXTRA, 0) * 100);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_PROGRESS);
		handler = new Handler(this);
		setContentView(R.layout.content);

		LocalBroadcastManager.getInstance(this).registerReceiver(scheduleLoadingProgressReceiver, new IntentFilter(FosdemApi.SCHEDULE_PROGRESS_ACTION));

		if (getSupportLoaderManager().getLoader(DOWNLOAD_SCHEDULE_LOADER_ID) != null) {
			// Reconnect to running loader
			startDownloadSchedule();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(scheduleLoadingProgressReceiver);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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

	private void startDownloadSchedule() {
		// Start by displaying indeterminate progress, determinate will come later
		setSupportProgressBarIndeterminate(true);
		setSupportProgressBarVisibility(true);
		getSupportLoaderManager().initLoader(DOWNLOAD_SCHEDULE_LOADER_ID, null, downloadScheduleLoaderCallbacks);
	}

	private static class DownloadScheduleLoader extends AbstractAsyncTaskLoader<Integer> {

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
}
