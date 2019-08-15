package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
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

	private ViewHolder mHolder;
	private boolean mIsProgressBarVisible;

	private final RecyclerView.AdapterDataObserver mEmptyObserver = new RecyclerView.AdapterDataObserver() {
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

	/**
	 * Override this method to provide a custom RecyclerView.
	 * The default one is using the theme's recyclerViewStyle.
	 */
	@NonNull
	protected RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
		return new RecyclerView(inflater.getContext());
	}

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
	 * Override this method to setup the RecyclerView (LayoutManager, ItemDecoration, ...)
	 */
	protected void onRecyclerViewCreated(RecyclerView recyclerView, @Nullable Bundle savedInstanceState) {
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final Context context = inflater.getContext();

		mHolder = new ViewHolder();

		mHolder.container = new FrameLayout(context);

		mHolder.recyclerView = onCreateRecyclerView(inflater, mHolder.container, savedInstanceState);
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
		// Ensure the RecyclerView and emptyObserver are properly unregistered from the adapter
		setAdapter(null);
		mHolder = null;
		mIsProgressBarVisible = false;
		super.onDestroyView();
	}

	/**
	 * Get the fragments's RecyclerView widget.
	 */
	public RecyclerView getRecyclerView() {
		final ViewHolder holder = mHolder;
		return (holder == null) ? null : holder.recyclerView;
	}

	/**
	 * Call this method to set the RecyclerView's adapter while ensuring the empty view
	 * will show or hide automatically according to the adapter's empty state.
	 */
	public void setAdapter(@Nullable RecyclerView.Adapter adapter) {
		final RecyclerView.Adapter oldAdapter = mHolder.recyclerView.getAdapter();
		if (oldAdapter == adapter) {
			return;
		}
		if (oldAdapter != null) {
			oldAdapter.unregisterAdapterDataObserver(mEmptyObserver);
		}
		mHolder.recyclerView.setAdapter(adapter);
		if (adapter != null) {
			adapter.registerAdapterDataObserver(mEmptyObserver);
		}

		updateEmptyViewVisibility();
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
				mHolder.recyclerView.setVisibility(View.VISIBLE);
				updateEmptyViewVisibility();
				mHolder.progress.hide();
			}
		}
	}
}