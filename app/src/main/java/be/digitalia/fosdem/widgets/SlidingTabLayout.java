/*
 * Copyright 2014 Chris Banes
 * Copyright 2016 Christophe Beyls
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.digitalia.fosdem.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import be.digitalia.fosdem.R;

/**
 */
public class SlidingTabLayout extends HorizontalScrollView {

	public interface TabListener {

		void onTabSelected(int pos);

		void onTabReSelected(int pos);

	}

	private static final int[][] TAB_COLOR_STATES = new int[][]{SELECTED_STATE_SET, EMPTY_STATE_SET};

	private int mTitleOffset;

	private int mTabViewLayoutId;
	private int mTabViewTextViewId;
	private boolean mDistributeEvenly;

	private ColorStateList mTextColor;

	ViewPager mViewPager;
	PagerAdapter mAdapter;
	private final InternalViewPagerListener mPageChangeListener = new InternalViewPagerListener();
	private final PagerAdapterObserver mPagerAdapterObserver = new PagerAdapterObserver();

	TabListener mTabListener;

	final SlidingTabStrip mTabStrip;

	public SlidingTabLayout(Context context) {
		this(context, null);
	}

	public SlidingTabLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		// Disable the Scroll Bar
		setHorizontalScrollBarEnabled(false);
		// Make sure that the Tab Strips fills this View
		setFillViewport(true);

		mTabStrip = new SlidingTabStrip(context);
		addView(mTabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingTabLayout,
				defStyle, R.style.SlidingTabLayout);

		mTabStrip.setSelectedIndicatorHeight(a.getDimensionPixelSize(R.styleable.SlidingTabLayout_indicatorHeight, 0));
		mTabStrip.setSelectedIndicatorColor(a.getColor(R.styleable.SlidingTabLayout_indicatorColor, 0));
		mTextColor = a.getColorStateList(R.styleable.SlidingTabLayout_textColor);
		if ((mTextColor != null) && a.hasValue(R.styleable.SlidingTabLayout_selectedTextColor)) {
			setTabTextColors(mTextColor.getDefaultColor(), a.getColor(R.styleable.SlidingTabLayout_selectedTextColor, 0));
		}
		setContentInsetStart(a.getDimensionPixelSize(R.styleable.SlidingTabLayout_contentInsetStart, 0));
		setDistributeEvenly(a.getBoolean(R.styleable.SlidingTabLayout_distributeEvenly, false));

		a.recycle();
	}

	public void setContentInsetStart(int offset) {
		mTitleOffset = offset;
		mTabStrip.setPadding(offset, 0, 0, 0);
	}

	public void setDistributeEvenly(boolean distributeEvenly) {
		mDistributeEvenly = distributeEvenly;
	}

	/**
	 * Sets the color to be used for indicating the selected tab.
	 * This will override the style color.
	 */
	public void setSelectedTabIndicatorColor(@ColorInt int color) {
		mTabStrip.setSelectedIndicatorColor(color);
	}

	public void setSelectedTabIndicatorHeight(int height) {
		mTabStrip.setSelectedIndicatorHeight(height);
	}

	public void setTabTextColors(ColorStateList color) {
		mTextColor = color;
	}

	public void setTabTextColors(@ColorInt int normalColor, @ColorInt int selectedColor) {
		mTextColor = createColorStateList(normalColor, selectedColor);
	}

	private static ColorStateList createColorStateList(int defaultColor, int selectedColor) {
		final int[] colors = new int[]{selectedColor, defaultColor};
		return new ColorStateList(TAB_COLOR_STATES, colors);
	}

	/**
	 * Set the custom layout to be inflated for the tab views.
	 *
	 * @param layoutResId Layout id to be inflated
	 * @param textViewId  id of the {@link TextView} in the inflated view
	 */
	public void setCustomTabView(@LayoutRes int layoutResId, @IdRes int textViewId) {
		mTabViewLayoutId = layoutResId;
		mTabViewTextViewId = textViewId;
	}

	public void setTabListener(TabListener tabListener) {
		mTabListener = tabListener;
	}

	/**
	 * Sets the associated view pager. The ViewPager must have an adapter set.
	 * The SlidingTabLayout will then listen for changes and update the tabs automatically.
	 */
	public void setViewPager(ViewPager viewPager) {
		if (mViewPager != null) {
			mViewPager.removeOnPageChangeListener(mPageChangeListener);
			mAdapter.unregisterDataSetObserver(mPagerAdapterObserver);
		}
		if (viewPager != null) {
			PagerAdapter adapter = viewPager.getAdapter();
			if (adapter == null) {
				throw new IllegalArgumentException("ViewPager does not have a PagerAdapter set");
			}
			mViewPager = viewPager;
			mAdapter = adapter;
			mPageChangeListener.reset();
			viewPager.addOnPageChangeListener(mPageChangeListener);
			adapter.registerDataSetObserver(mPagerAdapterObserver);
		} else {
			mViewPager = null;
			mAdapter = null;
		}
		notifyDataSetChanged();
	}

	void notifyDataSetChanged() {
		mTabStrip.removeAllViews();
		if (mViewPager != null) {
			populateTabStrip();
		}
	}

	static void setSelectedCompat(View view, boolean selected) {
		final boolean becomeSelected = selected && !view.isSelected();
		view.setSelected(selected);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN && becomeSelected) {
			// Pre-JB we need to manually send the TYPE_VIEW_SELECTED event
			view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
		}
	}

	private void populateTabStrip() {
		final int adapterCount = mAdapter.getCount();
		final View.OnClickListener tabClickListener = new TabClickListener();
		final LayoutInflater inflater = LayoutInflater.from(getContext());
		final int currentItem = mViewPager.getCurrentItem();

		for (int i = 0; i < adapterCount; i++) {
			View tabView;
			TextView tabTitleView;

			if (mTabViewLayoutId != 0) {
				// If there is a custom tab view layout id set, try and inflate it
				tabView = inflater.inflate(mTabViewLayoutId, mTabStrip, false);
				tabTitleView = tabView.findViewById(mTabViewTextViewId);
				if (tabTitleView == null) {
					tabTitleView = (TextView) tabView;
				}
			} else {
				// Inflate our default tab layout
				tabView = inflater.inflate(R.layout.widget_sliding_tab_layout_text, mTabStrip, false);
				tabTitleView = (TextView) tabView;
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
					// Emulate Roboto Medium in previous Android versions
					tabTitleView.setTypeface(Typeface.DEFAULT_BOLD);
				}
			}
			if (mTextColor != null) {
				tabTitleView.setTextColor(mTextColor);
			}

			if (mDistributeEvenly) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
				lp.width = 0;
				lp.weight = 1;
			}

			tabTitleView.setText(mAdapter.getPageTitle(i));
			tabView.setFocusable(true);
			tabView.setOnClickListener(tabClickListener);

			mTabStrip.addView(tabView);
			if (i == currentItem) {
				setSelectedCompat(tabView, true);
			}
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (mViewPager != null) {
			scrollToTab(mViewPager.getCurrentItem(), 0);
		}
		announceSelectedTab(hasWindowFocus());
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		announceSelectedTab(hasWindowFocus);
	}

	private void announceSelectedTab(boolean hasWindowFocus) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && hasWindowFocus) {
			getHandler().post(new Runnable() {
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@Override
				public void run() {
					if (mViewPager != null && mAdapter != null && mAdapter.getCount() > 0) {
						CharSequence pageTitle = mAdapter.getPageTitle(mViewPager.getCurrentItem());
						if (!TextUtils.isEmpty(pageTitle)) {
							announceForAccessibility(pageTitle);
						}
					}
				}
			});
		}
	}

	void scrollToTab(int tabIndex, float positionOffset) {
		final int tabStripChildCount = mTabStrip.getChildCount();
		if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
			return;
		}

		View selectedChild = mTabStrip.getChildAt(tabIndex);
		if (selectedChild != null) {
			int targetScrollX = selectedChild.getLeft() +
					Math.round(positionOffset * selectedChild.getWidth()) - mTitleOffset;

			scrollTo(targetScrollX, 0);
		}
	}

	class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
		private int mScrollState;

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			int tabStripChildCount = mTabStrip.getChildCount();
			if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
				return;
			}

			mTabStrip.onViewPagerPageChanged(position, positionOffset);


			scrollToTab(position, positionOffset);
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			mScrollState = state;
		}

		@Override
		public void onPageSelected(int position) {
			if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
				mTabStrip.onViewPagerPageChanged(position, 0f);
				scrollToTab(position, 0);
			}
			final int childCount = mTabStrip.getChildCount();
			for (int i = 0; i < childCount; i++) {
				setSelectedCompat(mTabStrip.getChildAt(i), position == i);
			}
		}

		public void reset() {
			mScrollState = ViewPager.SCROLL_STATE_IDLE;
		}
	}

	class PagerAdapterObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			notifyDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			notifyDataSetChanged();
		}
	}

	class TabClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			final int childCount = mTabStrip.getChildCount();
			for (int i = 0; i < childCount; i++) {
				if (v == mTabStrip.getChildAt(i)) {
					final int previousPos = mViewPager.getCurrentItem();
					mViewPager.setCurrentItem(i);

					if (mTabListener != null) {
						if (previousPos != i) {
							mTabListener.onTabSelected(i);
						} else {
							mTabListener.onTabReSelected(i);
						}
					}

					return;
				}
			}
		}
	}


	static class SlidingTabStrip extends LinearLayout {

		private int mSelectedIndicatorHeight;
		private final Paint mSelectedIndicatorPaint;

		private int mSelectedPosition;
		private float mSelectionOffset;

		SlidingTabStrip(Context context) {
			super(context);
			setWillNotDraw(false);
			mSelectedIndicatorPaint = new Paint();
		}

		void setSelectedIndicatorColor(@ColorInt int color) {
			mSelectedIndicatorPaint.setColor(color);
			invalidate();
		}

		void setSelectedIndicatorHeight(int height) {
			mSelectedIndicatorHeight = height;
			invalidate();
		}

		void onViewPagerPageChanged(int position, float positionOffset) {
			mSelectedPosition = position;
			mSelectionOffset = positionOffset;
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			final int height = getHeight();
			final int childCount = getChildCount();

			// Thick colored underline below the current selection
			if (childCount > 0) {
				View selectedTitle = getChildAt(mSelectedPosition);
				int left = selectedTitle.getLeft();
				int right = selectedTitle.getRight();

				if (mSelectionOffset > 0f && mSelectedPosition < (getChildCount() - 1)) {
					// Draw the selection partway between the tabs
					View nextTitle = getChildAt(mSelectedPosition + 1);
					left = (int) (mSelectionOffset * nextTitle.getLeft() +
							(1.0f - mSelectionOffset) * left);
					right = (int) (mSelectionOffset * nextTitle.getRight() +
							(1.0f - mSelectionOffset) * right);
				}

				canvas.drawRect(left, height - mSelectedIndicatorHeight, right,
						height, mSelectedIndicatorPaint);
			}
		}
	}
}