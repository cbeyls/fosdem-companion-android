package be.digitalia.fosdem.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.RoomImageDialogActivity;
import be.digitalia.fosdem.utils.ThemeUtils;

public class RoomImageDialogFragment extends DialogFragment {

	public static final String TAG = "room";

	private static final String ARG_ROOM_NAME = "roomName";
	private static final String ARG_ROOM_IMAGE_RESOURCE_ID = "imageResId";

	public static RoomImageDialogFragment newInstance(String roomName, @DrawableRes int imageResId) {
		RoomImageDialogFragment f = new RoomImageDialogFragment();
		Bundle args = new Bundle();
		args.putString(ARG_ROOM_NAME, roomName);
		args.putInt(ARG_ROOM_IMAGE_RESOURCE_ID, imageResId);
		f.setArguments(args);
		return f;
	}

	@NonNull
	@Override
	@SuppressLint("InflateParams")
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = requireArguments();

		AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());

		View contentView = LayoutInflater.from(dialogBuilder.getContext()).inflate(R.layout.dialog_room_image, null);
		final ImageView imageView = contentView.findViewById(R.id.room_image);
		if (!ThemeUtils.isLightTheme(imageView.getContext())) {
			ThemeUtils.invertImageColors(imageView);
		}
		imageView.setImageResource(args.getInt(ARG_ROOM_IMAGE_RESOURCE_ID));
		RoomImageDialogActivity.configureToolbar(this, contentView.findViewById(R.id.toolbar), args.getString(ARG_ROOM_NAME));

		Dialog dialog = dialogBuilder
				.setView(contentView)
				.create();
		Window window = dialog.getWindow();
		if (window != null) {
			window.getAttributes().windowAnimations = R.style.RoomImageDialogAnimations;
		}
		return dialog;
	}

	public void show(FragmentManager manager) {
		show(manager, TAG);
	}
}
