package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.widgets.ContentLoadingProgressBar;

/**
 * Fragment providing a RecyclerView, an empty view and a progress bar.
 *
 * @author Christophe Beyls
 */
public class RecyclerViewFragment extends Fragment {

	private static final float DEFAULT_EMPTY_VIEW_PADDING_DIPS = 16f;

	static class ViewHolder {
		FrameLayout container;
		RecyclerView recyclerView;
		View emptyView;
		ContentLoadingProgressBar progress;
	}

	private class EmptyViewAwareRecyclerView extends RecyclerView {

		private final AdapterDataObserver mEmptyObserver = new AdapterDataObserver() {
			@Override
			public void onChanged() {
				updateEmptyViewVisibility();
			}

			@Override
			public void onItemRangeInserted(int positionStart, int itemCount) {
				updateEmptyViewVisibility();
			}

			@Override
			public void onItemRangeRemoved(int positionStart, int itemCount) {
				updateEmptyViewVisibility();
			}
		};

		public EmptyViewAwareRecyclerView(Context context) {
			super(context);
		}

		public EmptyViewAwareRecyclerView(Context context, @Nullable AttributeSet attrs) {
			super(context, attrs);
		}

		public EmptyViewAwareRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		@Override
		public void setAdapter(Adapter adapter) {
			final Adapter oldAdapter = getAdapter();
			if (oldAdapter != null) {
				oldAdapter.unregisterAdapterDataObserver(mEmptyObserver);
			}
			super.setAdapter(adapter);
			if (adapter != null) {
				adapter.registerAdapterDataObserver(mEmptyObserver);
			}

			updateEmptyViewVisibility();
		}
	}

	private ViewHolder mHolder;
	private boolean mIsProgressBarVisible;

	/**
	 * Override this method to provide a custom Empty View.
	 * The default one is a TextView with some padding.
	 */
	@NonNull
	protected View onCreateEmptyView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
		TextView textView = new TextView(inflater.getContext());
		textView.setGravity(Gravity.CENTER);
		int textPadding = (int) (getResources().getDisplayMetrics().density * DEFAULT_EMPTY_VIEW_PADDING_DIPS + 0.5f);
		textView.setPadding(textPadding, textPadding, textPadding, textPadding);
		return textView;
	}

	/**
	 * Override this method to setup the RecyclerView (LayoutManager, ItemDecoration, Adapter)
	 */
	protected void onRecyclerViewCreated(RecyclerView recyclerView, @Nullable Bundle savedInstanceState) {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final Context context = inflater.getContext();

		mHolder = new ViewHolder();

		mHolder.container = new FrameLayout(context);

		mHolder.recyclerView = new EmptyViewAwareRecyclerView(context, null, R.attr.recyclerViewStyle);
		mHolder.recyclerView.setId(android.R.id.list);
		mHolder.recyclerView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
		mHolder.recyclerView.setHasFixedSize(true);
		mHolder.container.addView(mHolder.recyclerView,
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		mHolder.emptyView = onCreateEmptyView(inflater, mHolder.container, savedInstanceState);
		mHolder.emptyView.setId(android.R.id.empty);
		mHolder.emptyView.setVisibility(View.GONE);
		mHolder.container.addView(mHolder.emptyView,
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		mHolder.progress = new ContentLoadingProgressBar(context, null, android.R.attr.progressBarStyleLarge);
		mHolder.progress.setId(android.R.id.progress);
		mHolder.progress.hide();
		mHolder.container.addView(mHolder.progress,
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

		mHolder.container.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		onRecyclerViewCreated(mHolder.recyclerView, savedInstanceState);

		return mHolder.container;
	}

	@Override
	public void onDestroyView() {
		// Ensure the RecyclerView is properly unregistered as an observer of the adapter
		mHolder.recyclerView.setAdapter(null);
		mHolder = null;
		mIsProgressBarVisible = false;
		super.onDestroyView();
	}

	/**
	 * Get the fragments's RecyclerView widget.
	 */
	public RecyclerView getRecyclerView() {
		return mHolder.recyclerView;
	}

	/**
	 * The default content for a RecyclerViewFragment has a TextView that can be shown when the list is empty.
	 * Call this method to supply the text it should use.
	 */
	public void setEmptyText(CharSequence text) {
		((TextView) mHolder.emptyView).setText(text);
	}

	void updateEmptyViewVisibility() {
		if (!mIsProgressBarVisible) {
			RecyclerView.Adapter adapter = mHolder.recyclerView.getAdapter();
			final boolean isEmptyViewVisible = (adapter != null) && (adapter.getItemCount() == 0);
			mHolder.recyclerView.setVisibility(isEmptyViewVisible ? View.GONE : View.VISIBLE);
			mHolder.emptyView.setVisibility(isEmptyViewVisible ? View.VISIBLE : View.GONE);
		}
	}

	/**
	 * Call this method to show or hide the indeterminate progress bar.
	 * When shown, the RecyclerView will be hidden.
	 *
	 * @param visible true to show the progress bar, false to hide it. The initial value is false.
	 */
	public void setProgressBarVisible(boolean visible) {
		if (mIsProgressBarVisible != visible) {
			mIsProgressBarVisible = visible;

			if (visible) {
				mHolder.recyclerView.setVisibility(View.GONE);
				mHolder.emptyView.setVisibility(View.GONE);
				mHolder.progress.show();
			} else {
				updateEmptyViewVisibility();
				mHolder.progress.hide();
			}
		}
	}
}