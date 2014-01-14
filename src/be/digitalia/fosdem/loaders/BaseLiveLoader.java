package be.digitalia.fosdem.loaders;

import android.content.Context;
import android.os.Handler;

/**
 * A cursor loader which also automatically refreshes its data at a specified interval.
 * 
 * @author Christophe Beyls
 * 
 */
public abstract class BaseLiveLoader extends SimpleCursorLoader {

	private static final long REFRESH_INTERVAL = 60L * 1000L; // 1 minute

	private final Handler handler;
	private final Runnable timeoutRunnable = new Runnable() {

		@Override
		public void run() {
			onContentChanged();
		}
	};

	public BaseLiveLoader(Context context) {
		super(context);
		this.handler = new Handler();
	}

	@Override
	protected void onForceLoad() {
		super.onForceLoad();
		handler.removeCallbacks(timeoutRunnable);
		handler.postDelayed(timeoutRunnable, REFRESH_INTERVAL);
	}

	@Override
	protected void onReset() {
		super.onReset();
		handler.removeCallbacks(timeoutRunnable);
	}
}
