package be.digitalia.fosdem.activities;

import android.support.v4.app.SafeLoadersUtils;
import android.support.v7.app.AppCompatActivity;

/**
 * Common activity code with fragment loaders fix.
 */
public abstract class BaseActivity extends AppCompatActivity {

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		// TODO Remove when support-fragment Loaders bug is fixed
		SafeLoadersUtils.onRetainCustomNonConfigurationInstance(this);
		return super.onRetainCustomNonConfigurationInstance();
	}
}
