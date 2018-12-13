package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.content.Loader;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.BaseLiveLoader;

public class NextLiveListFragment extends BaseLiveListFragment {

	@Override
	protected String getEmptyText() {
		return getString(R.string.next_empty);
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new NextLiveLoader(getActivity());
	}

	private static class NextLiveLoader extends BaseLiveLoader {

		private static final long INTERVAL = 30L * 60L * 1000L; // 30 minutes

		public NextLiveLoader(Context context) {
			super(context);
		}

		@Override
		protected Cursor getCursor() {
			long now = System.currentTimeMillis();
			return DatabaseManager.getInstance().getEvents(now, now + INTERVAL, -1L, true);
		}
	}
}
