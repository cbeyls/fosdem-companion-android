package be.digitalia.fosdem.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

public class MessageDialogFragment extends DialogFragment {

	public static MessageDialogFragment newInstance(int titleResId, int messageResId) {
		MessageDialogFragment f = new MessageDialogFragment();
		Bundle args = new Bundle();
		args.putInt("titleResId", titleResId);
		args.putInt("messageResId", messageResId);
		f.setArguments(args);
		return f;
	}

	public static MessageDialogFragment newInstance(int titleResId, String message) {
		MessageDialogFragment f = new MessageDialogFragment();
		Bundle args = new Bundle();
		args.putInt("titleResId", titleResId);
		args.putString("message", message);
		f.setArguments(args);
		return f;
	}

	public static MessageDialogFragment newInstance(String title, String message) {
		MessageDialogFragment f = new MessageDialogFragment();
		Bundle args = new Bundle();
		args.putString("title", title);
		args.putString("message", message);
		f.setArguments(args);
		return f;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		int titleResId = args.getInt("titleResId", -1);
		String title = (titleResId != -1) ? getString(titleResId) : args.getString("title");
		int messageResId = args.getInt("messageResId", -1);
		String message = (messageResId != -1) ? getString(messageResId) : args.getString("message");

		return new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, null).create();
	}

	public void show(FragmentManager manager) {
		show(manager, "message");
	}
}
