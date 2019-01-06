package be.digitalia.fosdem.livedata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

public class ExtraTransformations {

	static final Object NOT_SET = new Object();

	private ExtraTransformations() {
	}

	public static <T1, T2> LiveData<Pair<T1, T2>> zipLatest(@NonNull LiveData<T1> l1, @NonNull LiveData<T2> l2) {
		final MediatorLiveData<Pair<T1, T2>> result = new MediatorLiveData<>();
		final Object[] latestValues = new Object[]{NOT_SET, NOT_SET};
		result.addSource(l1, new Observer<T1>() {
			@Override
			public void onChanged(@Nullable T1 v1) {
				Object v2 = latestValues[1];
				if (v2 != NOT_SET) {
					latestValues[1] = NOT_SET;
					result.setValue(Pair.create(v1, (T2) v2));
				} else {
					latestValues[0] = v1;
				}
			}
		});
		result.addSource(l2, new Observer<T2>() {
			@Override
			public void onChanged(@Nullable T2 v2) {
				Object v1 = latestValues[0];
				if (v1 != NOT_SET) {
					latestValues[0] = NOT_SET;
					result.setValue(Pair.create((T1) v1, v2));
				} else {
					latestValues[1] = v2;
				}
			}
		});
		return result;
	}
}
