@file:Suppress("DEPRECATION")

package be.digitalia.fosdem.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.CreateNdefMessageCallback
import be.digitalia.fosdem.model.Event
import java.nio.ByteBuffer

/**
 * NFC helper methods.
 *
 * @author Christophe Beyls
 */

/**
 * Call this method in an Activity, between onCreate() and onDestroy(), to make its content shareable using Android Beam if available.
 * Declare the corresponding MIME type of the NDEF record it in your Manifest's intent filters as the data type with an action of
 * android.nfc.action.NDEF_DISCOVERED to handle the NFC Intents on the receiver side.
 *
 * @return true if NFC is available and the content was made available, false if not.
 */
fun Activity.setNfcAppDataPushMessageCallbackIfAvailable(callback: CreateNfcAppDataCallback): Boolean {
    val adapter = NfcAdapter.getDefaultAdapter(applicationContext) ?: return false
    val packageName = packageName
    adapter.setNdefPushMessageCallback(CreateNdefMessageCallback {
        val appData = callback.createNfcAppData() ?: return@CreateNdefMessageCallback null
        val records = arrayOf(appData, NdefRecord.createApplicationRecord(packageName))
        NdefMessage(records)
    }, this)
    return true
}

fun Event.toNfcAppData(context: Context): NdefRecord {
    val mimeType = "application/${context.packageName}"
    val mimeData = id.toString().toByteArray()
    return NdefRecord.createMime(mimeType, mimeData)
}

fun NdefRecord.toEventIdString(): String {
    return String(payload)
}

fun List<Event>.toBookmarksNfcAppData(context: Context): NdefRecord {
    val mimeType = "application/${context.packageName}-bookmarks"
    val size = size
    val buffer = ByteBuffer.allocate(4 + size * 8)
    buffer.putInt(size)
    for (event in this) {
        buffer.putLong(event.id)
    }
    return NdefRecord.createMime(mimeType, buffer.array())
}

fun NdefRecord.toBookmarks(): LongArray? {
    return try {
        val buffer = ByteBuffer.wrap(payload)
        LongArray(buffer.int) { buffer.long }
    } catch (e: Exception) {
        null
    }
}

/**
 * Determines if the intent contains NFC NDEF application-specific data to be extracted.
 */
fun Intent.hasNfcAppData(): Boolean {
    return action == NfcAdapter.ACTION_NDEF_DISCOVERED
}

/**
 * Extracts application-specific data sent through NFC from an intent.
 * You must first ensure that the intent contains NFC data by calling hasAppData().
 *
 * @return The extracted app data as an NdefRecord
 */
fun Intent.extractNfcAppData(): NdefRecord {
    val rawMsgs = getParcelableArrayExtraCompat<NdefMessage>(NfcAdapter.EXTRA_NDEF_MESSAGES)!!
    val msg = rawMsgs[0] as NdefMessage
    return msg.records[0]
}

/**
 * Implement this interface to create application-specific data to be shared through Android Beam.
 */
interface CreateNfcAppDataCallback {
    /**
     * @return The app data, or null if no data is currently available for sharing.
     */
    fun createNfcAppData(): NdefRecord?
}