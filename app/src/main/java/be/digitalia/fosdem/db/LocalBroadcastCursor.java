package be.digitalia.fosdem.db;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * A cursor notifying its observers when a local broadcast matches the provided IntentFilter.
 * This is more efficient and more customizable than using the ContentResolver to notify changes.
 *
 * @author Christophe Beyls
 */
public class LocalBroadcastCursor extends CursorWrapper {

	final ContentObservable contentObservable = new ContentObservable();

	private final LocalBroadcastManager localBroadcastManager;
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		@SuppressWarnings("deprecation")
		public void onReceive(Context context, Intent intent) {
			if (matchIntent(context, intent)) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					contentObservable.dispatchChange(false, null);
				} else {
					contentObservable.dispatchChange(false);
				}
			}
		}
	};

	public LocalBroadcastCursor(Cursor wrappedCursor, Context context, IntentFilter intentFilter) {
		super(wrappedCursor);
		localBroadcastManager = LocalBroadcastManager.getInstance(context);
		localBroadcastManager.registerReceiver(receiver, intentFilter);
	}

	@Override
	public void registerContentObserver(ContentObserver observer) {
		contentObservable.registerObserver(observer);
	}

	@Override
	public void unregisterContentObserver(ContentObserver observer) {
		// cursor will unregister all observers when it closes
		if (!isClosed()) {
			contentObservable.unregisterObserver(observer);
		}
	}

	@Override
	public void setNotificationUri(ContentResolver cr, Uri uri) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Uri getNotificationUri() {
		return null;
	}

	@Override
	public void close() {
		super.close();
		contentObservable.unregisterAll();
		localBroadcastManager.unregisterReceiver(receiver);
	}

	/**
	 * Override this method to implement custom Intent matching in addition to the IntentFilter.
	 *
	 * @return True if the Intent matches and observers should be notified. Default is true.
	 */
	protected boolean matchIntent(Context context, Intent intent) {
		return true;
	}
}
