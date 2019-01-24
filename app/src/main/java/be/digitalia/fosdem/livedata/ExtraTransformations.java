package be.digitalia.fosdem.livedata;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

public class ExtraTransformations {

	private ExtraTransformations() {
	}

	@MainThread
	@NonNull
	public static <X> LiveData<X> distinctUntilChanged(@NonNull LiveData<X> source) {
		final MediatorLiveData<X> outputLiveData = new MediatorLiveData<>();
		outputLiveData.addSource(source, new Observer<X>() {

			boolean mFirstTime = true;

			@Override
			public void onChanged(X currentValue) {
				final X previousValue = outputLiveData.getValue();
				if (mFirstTime
						|| (previousValue == null && currentValue != null)
						|| (previousValue != null && !previousValue.equals(currentValue))) {
					mFirstTime = false;
					outputLiveData.setValue(currentValue);
				}
			}
		});
		return outputLiveData;
	}
}
