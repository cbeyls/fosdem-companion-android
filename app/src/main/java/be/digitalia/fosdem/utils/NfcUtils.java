package be.digitalia.fosdem.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import be.digitalia.fosdem.model.Event;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * NFC helper methods.
 *
 * @author Christophe Beyls
 */
public class NfcUtils {

	/**
	 * Implement this interface to create application-specific data to be shared through Android Beam.
	 */
	public interface CreateNfcAppDataCallback {
		/**
		 * @return The app data, or null if no data is currently available for sharing.
		 */
		@Nullable
		NdefRecord createNfcAppData();
	}

	/**
	 * Call this method in an Activity, between onCreate() and onDestroy(), to make its content sharable using Android Beam if available.
	 * Declare the corresponding MIME type of the NDEF record it in your Manifest's intent filters as the data type with an action of
	 * android.nfc.action.NDEF_DISCOVERED to handle the NFC Intents on the receiver side.
	 *
	 * @return true if NFC is available and the content was made available, false if not.
	 */
	@SuppressWarnings("deprecation")
	public static boolean setAppDataPushMessageCallbackIfAvailable(Activity activity, final CreateNfcAppDataCallback callback) {
		NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
		if (adapter == null) {
			return false;
		}
		final String packageName = activity.getPackageName();
		adapter.setNdefPushMessageCallback(event -> {
			final NdefRecord appData = callback.createNfcAppData();
			if (appData == null) {
				return null;
			}
			NdefRecord[] records = new NdefRecord[]{appData, NdefRecord.createApplicationRecord(packageName)};
			return new NdefMessage(records);
		}, activity);
		return true;
	}

	public static NdefRecord createEventAppData(@NonNull Context context, @NonNull Event event) {
		String mimeType = "application/" + context.getPackageName();
		byte[] mimeData = String.valueOf(event.getId()).getBytes();
		return NdefRecord.createMime(mimeType, mimeData);
	}

	public static String toEventIdString(@NonNull NdefRecord record) {
		return new String(record.getPayload());
	}

	public static NdefRecord createBookmarksAppData(@NonNull Context context, List<Event> bookmarks) {
		String mimeType = "application/" + context.getPackageName() + "-bookmarks";
		final int size = bookmarks.size();
		ByteBuffer buffer = ByteBuffer.allocate(4 + size * 8);
		buffer.putInt(size);
		for (int i = 0; i < size; ++i) {
			buffer.putLong(bookmarks.get(i).getId());
		}
		return NdefRecord.createMime(mimeType, buffer.array());
	}

	@Nullable
	public static long[] toBookmarks(@NonNull NdefRecord ndefRecord) {
		try {
			ByteBuffer buffer = ByteBuffer.wrap(ndefRecord.getPayload());
			final int size = buffer.getInt();
			long[] bookmarks = new long[size];
			for (int i = 0; i < size; ++i) {
				bookmarks[i] = buffer.getLong();
			}
			return bookmarks;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Determines if the intent contains NFC NDEF application-specific data to be extracted.
	 */
	public static boolean hasAppData(Intent intent) {
		return NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction());
	}

	/**
	 * Extracts application-specific data sent through NFC from an intent.
	 * You must first ensure that the intent contains NFC data by calling hasAppData().
	 *
	 * @return The extracted app data as an NdefRecord
	 */
	public static NdefRecord extractAppData(Intent intent) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		return msg.getRecords()[0];
	}
}
