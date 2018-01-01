package be.digitalia.fosdem.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageView;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.api.FosdemUrls;
import be.digitalia.fosdem.utils.StringUtils;

/**
 * A special Activity which is displayed like a dialog and shows a room image.
 * Specify the room name and the room image id as Intent extras.
 *
 * @author Christophe Beyls
 */
public class RoomImageDialogActivity extends BaseActivity {

	public static final String EXTRA_ROOM_NAME = "roomName";
	public static final String EXTRA_ROOM_IMAGE_RESOURCE_ID = "imageResId";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		final String roomName = intent.getStringExtra(EXTRA_ROOM_NAME);
		setTitle(roomName);

		setContentView(R.layout.dialog_room_image);
		((ImageView) findViewById(R.id.room_image)).setImageResource(intent.getIntExtra(EXTRA_ROOM_IMAGE_RESOURCE_ID, 0));
		configureToolbar(this, (Toolbar) findViewById(R.id.toolbar), roomName);
	}

	public static void configureToolbar(final Activity context, final Toolbar toolbar, final String roomName) {
		toolbar.setTitle(roomName);
		if (!TextUtils.isEmpty(roomName)) {
			toolbar.inflateMenu(R.menu.room_image_dialog);
			toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					switch (item.getItemId()) {
						case R.id.navigation:
							String localNavigationUrl = FosdemUrls.getLocalNavigationToLocation(StringUtils.toSlug(roomName));
							try {
								new CustomTabsIntent.Builder()
										.setToolbarColor(ContextCompat.getColor(context, R.color.color_primary))
										.setShowTitle(true)
										.build()
										.launchUrl(context, Uri.parse(localNavigationUrl));
							} catch (ActivityNotFoundException ignore) {
							}
							break;
					}
					return false;
				}
			});
		}
	}
}
