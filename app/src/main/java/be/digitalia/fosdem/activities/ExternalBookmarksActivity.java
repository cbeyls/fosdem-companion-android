package be.digitalia.fosdem.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.fragments.ExternalBookmarksListFragment;
import be.digitalia.fosdem.utils.NfcUtils;

public class ExternalBookmarksActivity extends SimpleToolbarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			Intent intent = getIntent();
			long[] bookmarkIds = null;
			if (NfcUtils.hasAppData(intent)) {
				bookmarkIds = NfcUtils.toBookmarks(NfcUtils.extractAppData(intent));
			}
			if (bookmarkIds == null) {
				// Invalid data format, exit
				finish();
				return;
			}

			Fragment f = ExternalBookmarksListFragment.newInstance(bookmarkIds);
			getSupportFragmentManager().beginTransaction().add(R.id.content, f).commit();
		}
	}
}
