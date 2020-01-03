package be.digitalia.fosdem.utils;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

public class RecyclerViewUtils {

	@NonNull
	public static RecyclerView getRecyclerView(@NonNull ViewPager2 viewPager) {
		return (RecyclerView) viewPager.getChildAt(0);
	}

	public static void enforceSingleScrollDirection(@NonNull RecyclerView recyclerView) {
		final SingleScrollDirectionEnforcer enforcer = new SingleScrollDirectionEnforcer();
		recyclerView.addOnItemTouchListener(enforcer);
		recyclerView.addOnScrollListener(enforcer);
	}

	private static class SingleScrollDirectionEnforcer extends RecyclerView.OnScrollListener
			implements RecyclerView.OnItemTouchListener {

		private int scrollState = RecyclerView.SCROLL_STATE_IDLE;
		private int scrollPointerId = -1;
		private int initialTouchX;
		private int initialTouchY;
		private int dx;
		private int dy;

		@Override
		public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
			final int action = e.getActionMasked();
			switch (action) {
				case MotionEvent.ACTION_DOWN:
					scrollPointerId = e.getPointerId(0);
					initialTouchX = (int) (e.getX() + 0.5f);
					initialTouchY = (int) (e.getY() + 0.5f);
					break;

				case MotionEvent.ACTION_POINTER_DOWN:
					final int actionIndex = e.getActionIndex();
					scrollPointerId = e.getPointerId(actionIndex);
					initialTouchX = (int) (e.getX(actionIndex) + 0.5f);
					initialTouchY = (int) (e.getY(actionIndex) + 0.5f);
					break;

				case MotionEvent.ACTION_MOVE: {
					final int index = e.findPointerIndex(scrollPointerId);
					if (index >= 0 && scrollState != RecyclerView.SCROLL_STATE_DRAGGING) {
						final int x = (int) (e.getX(index) + 0.5f);
						final int y = (int) (e.getY(index) + 0.5f);
						dx = x - initialTouchX;
						dy = y - initialTouchY;
					}
				}
			}
			return false;
		}

		@Override
		public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
		}

		@Override
		public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
		}

		@Override
		public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
			int oldState = scrollState;
			scrollState = newState;
			if (oldState == RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
				final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
				if (layoutManager != null) {
					final boolean canScrollHorizontally = layoutManager.canScrollHorizontally();
					final boolean canScrollVertically = layoutManager.canScrollVertically();
					if (canScrollHorizontally != canScrollVertically) {
						if (canScrollHorizontally && Math.abs(dy) > Math.abs(dx)) {
							recyclerView.stopScroll();
						}
						if (canScrollVertically && Math.abs(dx) > Math.abs(dy)) {
							recyclerView.stopScroll();
						}
					}
				}
			}
		}
	}
}
