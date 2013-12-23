package be.digitalia.fosdem.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * A Loader working with a global application cache instead of a local cache.
 * This allows to avoid starting a background thread if the result is already in cache.
 * You do NOT need to destroy this loader after the result has been delivered.
 * The cache will be checked each time the fragment is started.
 * 
 * @author Christophe Beyls
 */
public abstract class GlobalCacheLoader<T> extends AsyncTaskLoader<T> {

	public GlobalCacheLoader(Context context) {
		super(context);
	}

	@Override
	protected void onStartLoading() {
		T cachedResult = getCachedResult();
		if (cachedResult != null) {
			// If we currently have a result available, deliver it
			// immediately.
			deliverResult(cachedResult);
		}

		if (takeContentChanged() || cachedResult == null) {
			// If the data has changed since the last time it was loaded
			// or is not currently available, start a load.
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		// Attempt to cancel the current load task if possible.
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();

		onStopLoading();
	}

	@Override
	public void deliverResult(T data) {
		if (isStarted()) {
			// If the Loader is currently started, we can immediately
			// deliver its results.
			super.deliverResult(data);
		}
	}
	
	protected abstract T getCachedResult();
}
