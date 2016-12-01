package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import be.digitalia.fosdem.widgets.ContentLoadingProgressBar;

/**
 * Simpler ListFragment using ContentLoadingProgressBar.
 *
 * @author Christophe Beyls
 */
public class SmoothListFragment extends Fragment {

	private static final int DEFAULT_EMPTY_VIEW_PADDING_DIPS = 16;

	static class ViewHolder {
		FrameLayout container;
		View emptyView;
		ListView listView;
		ContentLoadingProgressBar progress;
	}

	private final AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			onListItemClick((ListView) parent, v, position, id);
		}
	};

	private ViewHolder mHolder;
	private ListAdapter mAdapter;
	private boolean mListShown;

	/**
	 * Override this method to provide a custom Empty View.
	 * The default one is a TextView with some padding.
	 */
	@NonNull
	protected View onCreateEmptyView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
		TextView textView = new TextView(inflater.getContext());
		textView.setGravity(Gravity.CENTER);
		int textPadding = (int) (getResources().getDisplayMetrics().density * DEFAULT_EMPTY_VIEW_PADDING_DIPS);
		textView.setPadding(textPadding, textPadding, textPadding, textPadding);
		return textView;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final Context context = inflater.getContext();

		mHolder = new ViewHolder();

		mHolder.container = new FrameLayout(context);

		mHolder.emptyView = onCreateEmptyView(inflater, mHolder.container, savedInstanceState);
		mHolder.emptyView.setId(android.R.id.empty);
		mHolder.container.addView(mHolder.emptyView,
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		mHolder.listView = new ListView(context);
		mHolder.listView.setId(android.R.id.list);
		mHolder.listView.setOnItemClickListener(mOnClickListener);
		mHolder.container.addView(mHolder.listView,
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		mHolder.progress = new ContentLoadingProgressBar(context, null, android.R.attr.progressBarStyleLarge);
		mHolder.progress.setId(android.R.id.progress);
		mHolder.progress.hide();
		mHolder.container.addView(mHolder.progress,
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

		mHolder.container.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		mListShown = true;

		if (mAdapter != null) {
			setListAdapter(mAdapter);
			mHolder.listView.setEmptyView(mHolder.emptyView);
		} else {
			setListShown(false);
		}

		return mHolder.container;
	}

	@Override
	public void onDestroyView() {
		// Ensure the ListView is properly unregistered as an observer of the adapter
		mHolder.listView.setAdapter(null);
		mHolder = null;
		mListShown = false;
		super.onDestroyView();
	}

	/**
	 * This method will be called when an item in the list is selected. Subclasses should override. Subclasses can call
	 * getListView().getItemAtPosition(position) if they need to access the data associated with the selected item.
	 *
	 * @param l        The ListView where the click happened
	 * @param v        The view that was clicked within the ListView
	 * @param position The position of the view in the list
	 * @param id       The row id of the item that was clicked
	 */
	public void onListItemClick(ListView l, View v, int position, long id) {
	}

	public void setListAdapter(ListAdapter adapter) {
		boolean hadNoAdapter = mAdapter == null;
		mAdapter = adapter;
		if (mHolder != null) {
			mHolder.listView.setAdapter(adapter);
			if (hadNoAdapter && !mListShown) {
				// The list was hidden, and previously didn't have an
				// adapter. It is now time to show it.
				setListShown(true);
			}
		}
	}

	/**
	 * Get the fragments's ListView widget.
	 */
	public ListView getListView() {
		return mHolder.listView;
	}

	/**
	 * The default content for a SmoothListFragment has a TextView that can be shown when the list is empty.
	 * Call this method to supply the text it should use.
	 */
	public void setEmptyText(CharSequence text) {
		((TextView) mHolder.emptyView).setText(text);
	}

	/**
	 * Control whether the list is being displayed. You can make it not displayed if you are waiting for the initial data to show in it. During this time an
	 * indeterminate progress indicator will be shown instead.
	 *
	 * @param shown If true, the list view is shown; if false, the progress indicator. The initial value is true.
	 */
	public void setListShown(boolean shown) {
		if (mListShown != shown) {
			if (shown) {
				mHolder.listView.setVisibility(View.VISIBLE);
				mHolder.listView.setEmptyView(mHolder.emptyView);
				mHolder.progress.hide();
			} else {
				mHolder.listView.setEmptyView(null);
				mHolder.listView.setVisibility(View.GONE);
				mHolder.emptyView.setVisibility(View.GONE);
				mHolder.progress.show();
			}

			mListShown = shown;
		}
	}

	/**
	 * Get the ListAdapter associated with this activity's ListView.
	 */
	public ListAdapter getListAdapter() {
		return mAdapter;
	}
}