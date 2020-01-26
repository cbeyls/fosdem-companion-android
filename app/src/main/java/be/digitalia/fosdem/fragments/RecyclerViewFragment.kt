package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import be.digitalia.fosdem.widgets.ContentLoadingProgressBar

/**
 * Fragment providing a RecyclerView, an empty view and a progress bar.
 *
 * @author Christophe Beyls
 */
open class RecyclerViewFragment : Fragment() {

    private class ViewHolder(val recyclerView: RecyclerView,
                             val emptyView: View,
                             val progress: ContentLoadingProgressBar)

    private var holder: ViewHolder? = null
    private val emptyObserver: AdapterDataObserver = object : AdapterDataObserver() {
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

    /**
     * Override this method to provide a custom RecyclerView.
     * The default one is using the theme's recyclerViewStyle.
     */
    protected open fun onCreateRecyclerView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        return RecyclerView(inflater.context)
    }

    /**
     * Override this method to provide a custom Empty View.
     * The default one is a TextView with some padding.
     */
    protected open fun onCreateEmptyView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        return TextView(inflater.context).apply {
            gravity = Gravity.CENTER
            val textPadding = (resources.displayMetrics.density * DEFAULT_EMPTY_VIEW_PADDING_DIPS + 0.5f).toInt()
            setPadding(textPadding, textPadding, textPadding, textPadding)
        }
    }

    /**
     * Override this method to setup the RecyclerView (LayoutManager, ItemDecoration, ...)
     */
    protected open fun onRecyclerViewCreated(recyclerView: RecyclerView, savedInstanceState: Bundle?) {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = inflater.context

        val subContainer = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val recyclerView = onCreateRecyclerView(inflater, subContainer, savedInstanceState).apply {
            id = android.R.id.list
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            setHasFixedSize(true)
            subContainer.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        val emptyView = onCreateEmptyView(inflater, subContainer, savedInstanceState).apply {
            id = android.R.id.empty
            isVisible = false
            subContainer.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        val progress = ContentLoadingProgressBar(context, null, android.R.attr.progressBarStyleLarge).apply {
            id = android.R.id.progress
            hide()
            subContainer.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }

        holder = ViewHolder(recyclerView, emptyView, progress)

        onRecyclerViewCreated(recyclerView, savedInstanceState)

        return subContainer
    }

    override fun onDestroyView() {
        // Ensure the RecyclerView and emptyObserver are properly unregistered from the adapter
        setAdapter(null)
        holder = null
        isProgressBarVisible = false
        super.onDestroyView()
    }

    /**
     * Get the fragments's RecyclerView widget.
     */
    val recyclerView: RecyclerView?
        get() = holder?.recyclerView

    /**
     * Call this method to set the RecyclerView's adapter while ensuring the empty view
     * will show or hide automatically according to the adapter's empty state.
     */
    fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        val viewHolder = requireNotNull(holder)
        val oldAdapter = viewHolder.recyclerView.adapter
        if (oldAdapter === adapter) {
            return
        }

        oldAdapter?.unregisterAdapterDataObserver(emptyObserver)
        viewHolder.recyclerView.adapter = adapter
        adapter?.registerAdapterDataObserver(emptyObserver)

        updateEmptyViewVisibility()
    }

    /**
     * The default content for a RecyclerViewFragment has a TextView that can be shown when the list is empty.
     * Call this method to supply the text it should use.
     */
    var emptyText: CharSequence?
        get() = (holder?.emptyView as? TextView)?.text
        set(value) {
            (holder?.emptyView as? TextView)?.text = value
        }

    private fun updateEmptyViewVisibility() {
        if (!isProgressBarVisible) {
            holder?.run {
                emptyView.isVisible = recyclerView.adapter?.itemCount == 0
            }
        }
    }

    /**
     * Set this field to show or hide the indeterminate progress bar.
     * When shown, the RecyclerView will be hidden.The initial value is false.
     */
    var isProgressBarVisible: Boolean = false
        set(visible) {
            if (field != visible) {
                field = visible
                holder?.run {
                    if (visible) {
                        recyclerView.isVisible = false
                        emptyView.isVisible = false
                        progress.show()
                    } else {
                        recyclerView.isVisible = true
                        updateEmptyViewVisibility()
                        progress.hide()
                    }
                }
            }
        }

    companion object {
        private const val DEFAULT_EMPTY_VIEW_PADDING_DIPS = 16f
    }
}