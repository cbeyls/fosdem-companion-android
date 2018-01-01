package be.digitalia.fosdem.utils;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Parcelable;

import java.nio.charset.Charset;

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
		byte[] createNfcAppData();
	}

	/**
	 * Call this method in an Activity, between onCreate() and onDestroy(), to make its content sharable using Android Beam if available. MIME type of the data
	 * to share will be "application/" followed by the app's package name. Declare it in your Manifest's intent filters as the data type with an action of
	 * android.nfc.action.NDEF_DISCOVERED to handle the NFC Intents on the receiver side.
	 *
	 * @return true if NFC is available and the content was made available, false if not.
	 */
	public static boolean setAppDataPushMessageCallbackIfAvailable(Activity activity, final CreateNfcAppDataCallback callback) {
		NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
		if (adapter == null) {
			return false;
		}
		final String packageName = activity.getPackageName();
		adapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {

			@Override
			public NdefMessage createNdefMessage(NfcEvent event) {
				byte[] appData = callback.createNfcAppData();
				if (appData == null) {
					return null;
				}
				NdefRecord[] records = new NdefRecord[]{createMimeRecord("application/" + packageName, appData),
						NdefRecord.createApplicationRecord(packageName)};
				return new NdefMessage(records);
			}

		}, activity);
		return true;
	}

	static NdefRecord createMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
		return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
	}

	/**
	 * Determines if the intent contains NFC NDEF application-specific data to be extracted.
	 */
	public static boolean hasAppData(Intent intent) {
		return NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction());
	}

	/**
	 * Extracts application-specific data sent through NFC from an intent. You must first ensure that the intent contains NFC data by calling hasAppData().
	 *
	 * @param intent
	 * @return The extracted data
	 */
	public static byte[] extractAppData(Intent intent) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		return msg.getRecords()[0].getPayload();
	}
}
