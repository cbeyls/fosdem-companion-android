package be.digitalia.fosdem.adapters

import android.util.SparseArray
import android.view.ViewGroup
import androidx.core.util.set
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import java.util.*

/**
 * Adapter which concatenates the items of multiple adapters.
 * Doesn't support stable ids, but properly delegates changes notifications.
 *
 *
 * Adapters may provide multiple view types but they must not overlap.
 * It's recommended to always use the item layout id as view type.
 *
 * @author Christophe Beyls
 */
class ConcatAdapter(vararg adapters: RecyclerView.Adapter<*>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    @Suppress("UNCHECKED_CAST")
    private val adapters = adapters as Array<RecyclerView.Adapter<RecyclerView.ViewHolder>>
    private val adapterObservers = Array<AdapterDataObserver>(adapters.size) { InternalObserver(it) }
    private val offsets = IntArray(adapters.size)
    private var totalItemCount = -1
    private val viewTypeAdapters = SparseArray<RecyclerView.Adapter<RecyclerView.ViewHolder>>()

    private inner class InternalObserver(private val adapterIndex: Int) : AdapterDataObserver() {

        override fun onChanged() {
            totalItemCount = -1
            notifyDataSetChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            if (totalItemCount != -1) {
                notifyItemRangeChanged(positionStart + offsets[adapterIndex], itemCount)
            }
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            if (totalItemCount != -1) {
                notifyItemRangeChanged(positionStart + offsets[adapterIndex], itemCount, payload)
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (totalItemCount != -1) {
                for (i in adapterIndex + 1 until offsets.size) {
                    offsets[i] += itemCount
                }
                totalItemCount += itemCount
                notifyItemRangeInserted(positionStart + offsets[adapterIndex], itemCount)
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            if (totalItemCount != -1) {
                for (i in adapterIndex + 1 until offsets.size) {
                    offsets[i] -= itemCount
                }
                totalItemCount -= itemCount
                notifyItemRangeRemoved(positionStart + offsets[adapterIndex], itemCount)
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            if (totalItemCount != -1) {
                val offset = offsets[adapterIndex]
                when {
                    itemCount == 1 -> {
                        notifyItemMoved(fromPosition + offset, toPosition + offset)
                    }
                    fromPosition < toPosition -> {
                        for (i in itemCount - 1 downTo 0) {
                            notifyItemMoved(fromPosition + i + offset, toPosition + i + offset)
                        }
                    }
                    else -> {
                        for (i in 0 until itemCount) {
                            notifyItemMoved(fromPosition + i + offset, toPosition + i + offset)
                        }
                    }
                }
            }
        }
    }

    private fun getAdapterIndexForPosition(position: Int): Int {
        var index = Arrays.binarySearch(offsets, position)
        if (index < 0) {
            return index.inv() - 1
        }
        // If the array contains multiple identical values (empty adapters), return the index of the last one
        do {
            ++index
        } while (index < offsets.size && offsets[index] == position)
        return --index
    }

    override fun getItemViewType(position: Int): Int {
        val index = getAdapterIndexForPosition(position)
        val adapter = adapters[index]
        val viewType = adapter.getItemViewType(position - offsets[index])
        if (viewTypeAdapters[viewType] == null) {
            viewTypeAdapters[viewType] = adapter
        }
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewTypeAdapters[viewType].onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val index = getAdapterIndexForPosition(position)
        adapters[index].onBindViewHolder(holder, position - offsets[index])
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        val index = getAdapterIndexForPosition(position)
        adapters[index].onBindViewHolder(holder, position - offsets[index], payloads)
    }

    override fun getItemCount(): Int {
        if (totalItemCount == -1) {
            var count = 0
            for (i in adapters.indices) {
                offsets[i] = count
                count += adapters[i].itemCount
            }
            totalItemCount = count
        }
        return totalItemCount
    }

    override fun registerAdapterDataObserver(observer: AdapterDataObserver) {
        if (!hasObservers()) {
            for (i in adapters.indices) {
                adapters[i].registerAdapterDataObserver(adapterObservers[i])
            }
        }
        super.registerAdapterDataObserver(observer)
    }

    override fun unregisterAdapterDataObserver(observer: AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        if (!hasObservers()) {
            for (i in adapters.indices) {
                adapters[i].unregisterAdapterDataObserver(adapterObservers[i])
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        for (adapter in adapters) {
            adapter.onAttachedToRecyclerView(recyclerView)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        for (adapter in adapters) {
            adapter.onDetachedFromRecyclerView(recyclerView)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        viewTypeAdapters[holder.itemViewType].onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        viewTypeAdapters[holder.itemViewType].onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        viewTypeAdapters[holder.itemViewType].onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        return viewTypeAdapters[holder.itemViewType].onFailedToRecycleView(holder)
    }
}