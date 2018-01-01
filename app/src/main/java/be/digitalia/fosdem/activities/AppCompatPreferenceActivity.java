package be.digitalia.fosdem.activities;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public class AppCompatPreferenceActivity extends PreferenceActivity {

	private AppCompatDelegate mDelegate;
	private int mThemeId = 0;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		final AppCompatDelegate delegate = getDelegate();
		delegate.installViewFactory();
		delegate.onCreate(savedInstanceState);
		if (delegate.applyDayNight() && mThemeId != 0) {
			// If DayNight has been applied, we need to re-apply the theme for
			// the changes to take effect. On API 23+, we should bypass
			// setTheme(), which will no-op if the theme ID is identical to the
			// current theme ID.
			if (Build.VERSION.SDK_INT >= 23) {
				onApplyThemeResource(getTheme(), mThemeId, false);
			} else {
				setTheme(mThemeId);
			}
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public void setTheme(@StyleRes final int resid) {
		super.setTheme(resid);
		// Keep hold of the theme id so that we can re-set it later if needed
		mThemeId = resid;
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		getDelegate().onPostCreate(savedInstanceState);
	}

	@Nullable
	public ActionBar getSupportActionBar() {
		return getDelegate().getSupportActionBar();
	}

	public void setSupportActionBar(@Nullable Toolbar toolbar) {
		getDelegate().setSupportActionBar(toolbar);
	}

	@Override
	@NonNull
	public MenuInflater getMenuInflater() {
		return getDelegate().getMenuInflater();
	}

	@Override
	public void setContentView(@LayoutRes int layoutResID) {
		getDelegate().setContentView(layoutResID);
	}

	@Override
	public void setContentView(View view) {
		getDelegate().setContentView(view);
	}

	@Override
	public void setContentView(View view, ViewGroup.LayoutParams params) {
		getDelegate().setContentView(view, params);
	}

	@Override
	public void addContentView(View view, ViewGroup.LayoutParams params) {
		getDelegate().addContentView(view, params);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getDelegate().onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		getDelegate().onPostResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
		getDelegate().onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		getDelegate().onStop();
	}

	@Override
	@Nullable
	public <T extends View> T findViewById(@IdRes int id) {
		return getDelegate().findViewById(id);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getDelegate().onDestroy();
	}

	@Override
	protected void onTitleChanged(CharSequence title, int color) {
		super.onTitleChanged(title, color);
		getDelegate().setTitle(title);
	}

	public boolean supportRequestWindowFeature(int featureId) {
		return getDelegate().requestWindowFeature(featureId);
	}

	@Override
	public void invalidateOptionsMenu() {
		getDelegate().invalidateOptionsMenu();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getDelegate().onSaveInstanceState(outState);
	}

	@NonNull
	public AppCompatDelegate getDelegate() {
		if (mDelegate == null) {
			mDelegate = AppCompatDelegate.create(this, null);
		}
		return mDelegate;
	}
}