package be.digitalia.fosdem.fragments

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.widgets.ContentLoadingViewMediator

class RecyclerViewViewHolder(view: View) {
    val recyclerView: RecyclerView = view.findViewById(android.R.id.list)
    private val emptyView: View = view.findViewById(android.R.id.empty)
    private val progress = ContentLoadingViewMediator(view.findViewById(android.R.id.progress))

    private val emptyObserver: RecyclerView.AdapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            updateEmptyViewVisibility()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateEmptyViewVisibility()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            updateEmptyViewVisibility()
        }
    }

    init {
        recyclerView.setHasFixedSize(true)
        progress.isVisible = false
    }

    /**
     * Call this method to set the RecyclerView's adapter while ensuring the empty view
     * will show or hide automatically according to the adapter's empty state.
     */
    fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        val oldAdapter = recyclerView.adapter
        if (oldAdapter === adapter) {
            return
        }

        oldAdapter?.unregisterAdapterDataObserver(emptyObserver)
        recyclerView.adapter = adapter
        adapter?.registerAdapterDataObserver(emptyObserver)

        updateEmptyViewVisibility()
    }

    /**
     * The default content for a RecyclerViewFragment has a TextView that can be shown when the list is empty.
     * Call this method to supply the text it should use.
     */
    var emptyText: CharSequence?
        get() = (emptyView as? TextView)?.text
        set(value) {
            (emptyView as? TextView)?.text = value
        }

    private fun updateEmptyViewVisibility() {
        emptyView.isVisible = !isProgressBarVisible && recyclerView.adapter?.itemCount == 0
    }

    /**
     * Set this field to show or hide the indeterminate progress bar.
     * When shown, the RecyclerView will be hidden.The initial value is false.
     */
    var isProgressBarVisible: Boolean = false
        set(visible) {
            if (field != visible) {
                field = visible
                recyclerView.isVisible = !visible
                progress.isVisible = visible
                updateEmptyViewVisibility()
            }
        }
}