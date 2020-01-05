package be.digitalia.fosdem.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.api.FosdemUrls;
import be.digitalia.fosdem.utils.CustomTabsUtils;
import be.digitalia.fosdem.utils.ThemeUtils;

public class MapFragment extends Fragment {

	private static final double DESTINATION_LATITUDE = 50.812375;
	private static final double DESTINATION_LONGITUDE = 4.380734;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_map, container, false);
		final ImageView imageView = view.findViewById(R.id.map);
		if (!ThemeUtils.isLightTheme(imageView.getContext())) {
			ThemeUtils.invertImageColors(imageView);
		}
		return view;
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.map, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.directions:
				launchDirections();
				return true;
			case R.id.navigation:
				launchLocalNavigation();
				return true;
		}
		return false;
	}

	private void launchDirections() {
		// Build intent to start Google Maps directions
		String uri = String.format(Locale.US,
				"https://maps.google.com/maps?f=d&daddr=%1$f,%2$f&dirflg=r",
				DESTINATION_LATITUDE, DESTINATION_LONGITUDE);

		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

		try {
			startActivity(intent);
		} catch (ActivityNotFoundException ignore) {
		}
	}

	private void launchLocalNavigation() {
		try {
			final Context context = requireContext();
			CustomTabsUtils.configureToolbarColors(new CustomTabsIntent.Builder(), context, R.color.light_color_primary)
					.setShowTitle(true)
					.build()
					.launchUrl(context, Uri.parse(FosdemUrls.getLocalNavigation()));
		} catch (ActivityNotFoundException ignore) {
		}
	}
}
