package be.digitalia.fosdem.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.api.FosdemUrls;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.utils.CustomTabsUtils;
import be.digitalia.fosdem.utils.StringUtils;
import be.digitalia.fosdem.utils.ThemeUtils;

/**
 * A special Activity which is displayed like a dialog and shows a room image.
 * Specify the room name and the room image id as Intent extras.
 *
 * @author Christophe Beyls
 */
public class RoomImageDialogActivity extends AppCompatActivity {

	public static final String EXTRA_ROOM_NAME = "roomName";
	public static final String EXTRA_ROOM_IMAGE_RESOURCE_ID = "imageResId";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		final String roomName = intent.getStringExtra(EXTRA_ROOM_NAME);
		setTitle(roomName);

		setContentView(R.layout.dialog_room_image);
		final ImageView imageView = findViewById(R.id.room_image);
		if (!ThemeUtils.isLightTheme(imageView.getContext())) {
			ThemeUtils.invertImageColors(imageView);
		}
		imageView.setImageResource(intent.getIntExtra(EXTRA_ROOM_IMAGE_RESOURCE_ID, 0));
		configureToolbar(this, findViewById(R.id.toolbar), roomName);
	}

	public static void configureToolbar(LifecycleOwner owner,
										final Toolbar toolbar, final String roomName) {
		toolbar.setTitle(roomName);
		if (!TextUtils.isEmpty(roomName)) {
			final Context context = toolbar.getContext();

			toolbar.inflateMenu(R.menu.room_image_dialog);
			toolbar.setOnMenuItemClickListener(item -> {
				switch (item.getItemId()) {
					case R.id.navigation:
						String localNavigationUrl = FosdemUrls.getLocalNavigationToLocation(StringUtils.toSlug(roomName));
						try {
							CustomTabsUtils.configureToolbarColors(new CustomTabsIntent.Builder(), context, R.color.light_color_primary)
									.setShowTitle(true)
									.build()
									.launchUrl(context, Uri.parse(localNavigationUrl));
						} catch (ActivityNotFoundException ignore) {
						}
						break;
				}
				return false;
			});

			// Display the room status as subtitle
			FosdemApi.getRoomStatuses(toolbar.getContext()).observe(owner, roomStatuses -> {
				RoomStatus roomStatus = roomStatuses.get(roomName);
				if (roomStatus != null) {
					SpannableString roomNameSpannable = new SpannableString(context.getString(roomStatus.getNameResId()));
					int color = ContextCompat.getColor(context, roomStatus.getColorResId());
					roomNameSpannable.setSpan(new ForegroundColorSpan(color),
							0, roomNameSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					toolbar.setSubtitle(roomNameSpannable);
				} else {
					toolbar.setSubtitle(null);
				}
			});
		}
	}
}
