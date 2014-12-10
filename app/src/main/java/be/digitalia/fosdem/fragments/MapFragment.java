package be.digitalia.fosdem.fragments;

import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import be.digitalia.fosdem.R;

public class MapFragment extends Fragment {

	private static final double DESTINATION_LATITUDE = 50.812375;
	private static final double DESTINATION_LONGITUDE = 4.380734;
	private static final String DESTINATION_NAME = "ULB";
	private static final String GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_map, container, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.map, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.directions:
			launchDirections();
			return true;
		}
		return false;
	}

	private void launchDirections() {
		// Build intent to start Google Maps directions
		String uri = String.format(Locale.US, "http://maps.google.com/maps?f=d&daddr=%1$f,%2$f(%3$s)&dirflg=r", DESTINATION_LATITUDE, DESTINATION_LONGITUDE,
				DESTINATION_NAME);

		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

		// If Google Maps app is found, don't allow to choose other apps to handle this intent
		List<ResolveInfo> resolveInfos = getActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfos != null) {
			for (ResolveInfo info : resolveInfos) {
				if (GOOGLE_MAPS_PACKAGE_NAME.equals(info.activityInfo.packageName)) {
					intent.setPackage(GOOGLE_MAPS_PACKAGE_NAME);
					break;
				}
			}
		}

		startActivity(intent);
	}
}
