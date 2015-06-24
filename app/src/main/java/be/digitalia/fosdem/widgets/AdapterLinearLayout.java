package be.digitalia.fosdem.widgets;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Vertical LinearLayout populated by a special adapter.
 *
 * @author Christophe Beyls
 */
public class AdapterLinearLayout extends LinearLayout {

	/**
	 * Implement this Adapter to populate the layout.
	 * Call notifyDataSetChanged() to update it.
	 */
	public static abstract class Adapter<T> {

		private final DataSetObservable mDataSetObservable = new DataSetObservable();

		private void registerDataSetObserver(DataSetObserver observer) {
			mDataSetObservable.registerObserver(observer);
		}

		private void unregisterDataSetObserver(DataSetObserver observer) {
			mDataSetObservable.unregisterObserver(observer);
		}

		public void notifyDataSetChanged() {
			mDataSetObservable.notifyChanged();
		}

		public abstract int getCount();

		public abstract T getItem(int position);

		public abstract View getView(int position, View convertView, ViewGroup parent);
	}

	private class AdapterLinearLayoutDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			populateFromAdapter();
		}
	}

	private Adapter mAdapter;
	private AdapterLinearLayoutDataSetObserver mDataSetObserver;


	public AdapterLinearLayout(Context context) {
		this(context, null);
	}

	public AdapterLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOrientation(VERTICAL);
	}

	public void setAdapter(Adapter adapter) {
		if (mAdapter == adapter) {
			return;
		}
		if (mAdapter != null && mDataSetObserver != null) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}
		removeAllViews();
		mAdapter = adapter;
		if (adapter != null && mDataSetObserver != null) {
			populateFromAdapter();
			adapter.registerDataSetObserver(mDataSetObserver);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mDataSetObserver = new AdapterLinearLayoutDataSetObserver();
		if (mAdapter != null) {
			populateFromAdapter();
			mAdapter.registerDataSetObserver(mDataSetObserver);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAdapter != null) {
			mAdapter.unregisterDataSetObserver(mDataSetObserver);
		}
		mDataSetObserver = null;
	}

	private void populateFromAdapter() {
		final Adapter adapter = mAdapter;
		final int currentCount = getChildCount();
		final int newCount = adapter.getCount();
		final int commonCount = Math.min(currentCount, newCount);
		// 1. Update common views
		for (int i = 0; i < commonCount; ++i) {
			final View currentView = getChildAt(i);
			final View newView = adapter.getView(i, currentView, this);
			if (currentView != newView) {
				// Edge case: View is not recycled
				removeViewAt(i);
				addView(newView, i);
			}
		}
		// 2a. Add missing views
		for (int i = commonCount; i < newCount; ++i) {
			addView(adapter.getView(i, null, this));
		}
		// 2b. Remove extra views (starting from the end to avoid array copies)
		for (int i = currentCount - 1; i >= commonCount; --i) {
			removeViewAt(i);
		}
	}
}
