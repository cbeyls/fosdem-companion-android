package be.digitalia.fosdem.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.RoomImageDialogActivity;

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
		Bundle args = getArguments();

		View contentView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_room_image, null);
		((ImageView) contentView.findViewById(R.id.room_image)).setImageResource(args.getInt(ARG_ROOM_IMAGE_RESOURCE_ID));
		Toolbar toolbar = (Toolbar) contentView.findViewById(R.id.toolbar);
		RoomImageDialogActivity.configureToolbar(toolbar, args.getString(ARG_ROOM_NAME));

		Dialog dialog = new AlertDialog.Builder(getActivity())
				.setView(contentView)
				.create();
		dialog.getWindow().getAttributes().windowAnimations = R.style.RoomImageDialogAnimations;
		return dialog;
	}

	public void show(FragmentManager manager) {
		show(manager, TAG);
	}
}
