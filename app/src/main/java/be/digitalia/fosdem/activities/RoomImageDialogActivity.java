package be.digitalia.fosdem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import be.digitalia.fosdem.R;

/**
 * A special Activity which is displayed like a dialog and shows a room image. Specify the room name and the room image id as Intent extras.
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

		setTitle(intent.getStringExtra(EXTRA_ROOM_NAME));

		ImageView imageView = new ImageView(this);
		imageView.setImageResource(intent.getIntExtra(EXTRA_ROOM_IMAGE_RESOURCE_ID, 0));
		int padding = getResources().getDimensionPixelSize(R.dimen.content_margin);
		imageView.setPadding(padding, padding, padding, padding);

		setContentView(imageView);
	}
}
