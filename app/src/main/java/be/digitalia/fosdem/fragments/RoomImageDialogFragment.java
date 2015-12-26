package be.digitalia.fosdem.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.widget.ImageView;

import be.digitalia.fosdem.R;

public class RoomImageDialogFragment extends DialogFragment {

	public static final String TAG = "room";

	public static RoomImageDialogFragment newInstance(String roomName, @DrawableRes int imageResId) {
		RoomImageDialogFragment f = new RoomImageDialogFragment();
		Bundle args = new Bundle();
		args.putString("roomName", roomName);
		args.putInt("imageResId", imageResId);
		f.setArguments(args);
		return f;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();

		ImageView imageView = new ImageView(getActivity());
		imageView.setImageResource(args.getInt("imageResId"));
		int padding = getResources().getDimensionPixelSize(R.dimen.content_margin);
		imageView.setPadding(padding, padding, padding, padding);

		Dialog dialog = new AlertDialog.Builder(getActivity())
				.setTitle(args.getString("roomName"))
				.setView(imageView).create();
		dialog.getWindow().getAttributes().windowAnimations = R.style.RoomImageDialogAnimations;
		return dialog;
	}

	public void show(FragmentManager manager) {
		show(manager, TAG);
	}
}
