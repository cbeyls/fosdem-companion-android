package android.support.v7.widget;

import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

/**
 * Adapter which concatenates the items of multiple adapters.
 * Doesn't support stable ids, but properly delegates changes notifications.
 * <p>
 * Adapters may provide multiple view types but they must not overlap.
 * It's recommended to always use the item layout id as view type.
 * <p>
 * Warning: You need to use ConcatAdapter.getAdapterPosition(ViewHolder)
 * in place of ViewHolder.getAdapterPosition() inside child adapters.
 *
 * @author Christophe Beyls
 */
public class ConcatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final RecyclerView.Adapter<RecyclerView.ViewHolder>[] adapters;
	private final RecyclerView.AdapterDataObserver[] adapterObservers;
	final int[] offsets;
	int totalItemCount = -1;
	private final SparseArray<RecyclerView.Adapter<RecyclerView.ViewHolder>> viewTypeAdapters = new SparseArray<>();

	private class InternalObserver extends RecyclerView.AdapterDataObserver {

		private final int adapterIndex;

		InternalObserver(int adapterIndex) {
			this.adapterIndex = adapterIndex;
		}

		@Override
		public void onChanged() {
			totalItemCount = -1;
			notifyDataSetChanged();
		}

		@Override
		public void onItemRangeChanged(int positionStart, int itemCount) {
			if (totalItemCount != -1) {
				notifyItemRangeChanged(positionStart + offsets[adapterIndex], itemCount);
			}
		}

		@Override
		public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
			if (totalItemCount != -1) {
				notifyItemRangeChanged(positionStart + offsets[adapterIndex], itemCount, payload);
			}
		}

		@Override
		public void onItemRangeInserted(int positionStart, int itemCount) {
			if (totalItemCount != -1) {
				for (int i = adapterIndex + 1, size = offsets.length; i < size; ++i) {
					offsets[i] += itemCount;
				}
				totalItemCount += itemCount;
				notifyItemRangeInserted(positionStart + offsets[adapterIndex], itemCount);
			}
		}

		@Override
		public void onItemRangeRemoved(int positionStart, int itemCount) {
			if (totalItemCount != -1) {
				for (int i = adapterIndex + 1, size = offsets.length; i < size; ++i) {
					offsets[i] -= itemCount;
				}
				totalItemCount -= itemCount;
				notifyItemRangeRemoved(positionStart + offsets[adapterIndex], itemCount);
			}
		}

		@Override
		public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
			if (totalItemCount != -1) {
				final int offset = offsets[adapterIndex];
				notifyItemMoved(fromPosition + offset, toPosition + offset);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public ConcatAdapter(RecyclerView.Adapter<? extends RecyclerView.ViewHolder>... adapters) {
		this.adapters = (RecyclerView.Adapter<RecyclerView.ViewHolder>[]) adapters;
		final int size = adapters.length;
		adapterObservers = new RecyclerView.AdapterDataObserver[size];
		for (int i = 0; i < size; ++i) {
			adapterObservers[i] = new InternalObserver(i);
		}
		offsets = new int[size];
	}

	/**
	 * @return The adapter position relative to the child adapter, if any.
	 */
	public static int getAdapterPosition(@NonNull RecyclerView.ViewHolder holder) {
		int position = holder.getAdapterPosition();
		if (position != RecyclerView.NO_POSITION) {
			RecyclerView.Adapter adapter = holder.mOwnerRecyclerView.getAdapter();
			if (adapter instanceof ConcatAdapter) {
				final int[] offsets = ((ConcatAdapter) adapter).offsets;
				final int index = Arrays.binarySearch(offsets, position);
				if (index >= 0) {
					position = 0;
				} else {
					position -= offsets[~index - 1];
				}
			}
		}
		return position;
	}

	private int getAdapterIndexForPosition(int position) {
		int index = Arrays.binarySearch(offsets, position);
		if (index < 0) {
			return ~index - 1;
		}
		// If the array contains multiple identical values (empty adapters), return the index of the last one
		do {
			++index;
		}
		while ((index < offsets.length) && (offsets[index] == position));
		return --index;
	}

	@Override
	public int getItemViewType(int position) {
		final int index = getAdapterIndexForPosition(position);
		RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = adapters[index];
		int viewType = adapter.getItemViewType(position - offsets[index]);
		if (viewTypeAdapters.get(viewType) == null) {
			viewTypeAdapters.put(viewType, adapter);
		}
		return viewType;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return viewTypeAdapters.get(viewType).onCreateViewHolder(parent, viewType);
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		final int index = getAdapterIndexForPosition(position);
		adapters[index].onBindViewHolder(holder, position - offsets[index]);
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
		final int index = getAdapterIndexForPosition(position);
		adapters[index].onBindViewHolder(holder, position - offsets[index], payloads);
	}

	@Override
	public int getItemCount() {
		if (totalItemCount == -1) {
			int count = 0;
			for (int i = 0, size = adapters.length; i < size; ++i) {
				offsets[i] = count;
				count += adapters[i].getItemCount();
			}
			totalItemCount = count;
		}
		return totalItemCount;
	}

	@Override
	public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
		if (!hasObservers()) {
			for (int i = 0, size = adapters.length; i < size; ++i) {
				adapters[i].registerAdapterDataObserver(adapterObservers[i]);
			}
		}
		super.registerAdapterDataObserver(observer);
	}

	@Override
	public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
		super.unregisterAdapterDataObserver(observer);
		if (!hasObservers()) {
			for (int i = 0, size = adapters.length; i < size; ++i) {
				adapters[i].unregisterAdapterDataObserver(adapterObservers[i]);
			}
		}
	}

	@Override
	public void onAttachedToRecyclerView(RecyclerView recyclerView) {
		for (RecyclerView.Adapter<RecyclerView.ViewHolder> adapter : adapters) {
			adapter.onAttachedToRecyclerView(recyclerView);
		}
	}

	@Override
	public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
		for (RecyclerView.Adapter<RecyclerView.ViewHolder> adapter : adapters) {
			adapter.onDetachedFromRecyclerView(recyclerView);
		}
	}

	@Override
	public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
		viewTypeAdapters.get(holder.getItemViewType()).onViewAttachedToWindow(holder);
	}

	@Override
	public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
		viewTypeAdapters.get(holder.getItemViewType()).onViewDetachedFromWindow(holder);
	}

	@Override
	public void onViewRecycled(RecyclerView.ViewHolder holder) {
		viewTypeAdapters.get(holder.getItemViewType()).onViewRecycled(holder);
	}

	@Override
	public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
		return viewTypeAdapters.get(holder.getItemViewType()).onFailedToRecycleView(holder);
	}
}
