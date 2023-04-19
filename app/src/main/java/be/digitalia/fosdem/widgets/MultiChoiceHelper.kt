package be.digitalia.fosdem.widgets

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.LongSparseArray
import android.util.SparseBooleanArray
import android.view.View
import android.widget.Checkable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.core.util.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.savedstate.SavedStateRegistryOwner
import be.digitalia.fosdem.utils.getParcelableCompat

/**
 * Helper class to reproduce ListView's modal MultiChoice mode with a RecyclerView.
 * Call setAdapter(adapter, adapterLifecycleOwner) on it as soon as the adapter data is populated.
 *
 * @author Christophe Beyls
 */
class MultiChoiceHelper(private val activity: AppCompatActivity, owner: SavedStateRegistryOwner, listener: MultiChoiceModeListener) {
    /**
     * A handy ViewHolder base class which works with the MultiChoiceHelper
     * and reproduces the default behavior of a ListView.
     */
    abstract class ViewHolder(itemView: View, private val multiChoiceHelper: MultiChoiceHelper) : RecyclerView.ViewHolder(itemView) {

        private var clickListener: View.OnClickListener? = null

        fun setOnClickListener(clickListener: View.OnClickListener?) {
            this.clickListener = clickListener
        }

        fun bindSelection() {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val isChecked = multiChoiceHelper.isItemChecked(position)
                val mainView = itemView
                if (mainView is Checkable) {
                    mainView.isChecked = isChecked
                } else {
                    mainView.isActivated = isChecked
                }
            }
        }

        val isMultiChoiceActive: Boolean
            get() = multiChoiceHelper.checkedItemCount > 0

        init {
            itemView.setOnClickListener { view ->
                if (isMultiChoiceActive) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        multiChoiceHelper.toggleItemChecked(position)
                    }
                } else {
                    clickListener?.onClick(view)
                }
            }
            itemView.setOnLongClickListener {
                if (isMultiChoiceActive) {
                    return@setOnLongClickListener false
                }
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    multiChoiceHelper.setItemChecked(position, true)
                }
                true
            }
        }
    }

    interface MultiChoiceModeListener : ActionMode.Callback {
        /**
         * Called when an item is checked or unchecked during selection mode.
         *
         * @param mode     The [ActionMode] providing the selection mode
         * @param position Adapter position of the item that was checked or unchecked
         * @param id       Adapter ID of the item that was checked or unchecked
         * @param checked  `true` if the item is now checked, `false`
         * if the item is now unchecked.
         */
        fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean)
    }

    private val multiChoiceModeCallback: MultiChoiceModeListener = MultiChoiceModeWrapper(listener)
    val checkedItemPositions: SparseBooleanArray
    private val checkedIdStates: LongSparseArray<Int>
    var checkedItemCount: Int
        private set
    var adapter: RecyclerView.Adapter<*>? = null
        private set
    private var adapterLifecycle: Lifecycle? = null
    private var choiceActionMode: ActionMode? = null

    /**
     * Make sure this constructor is called before setting the adapter on the RecyclerView
     * so this class will be notified before the RecyclerView in case of data set changes.
     */
    init {
        val restoreBundle = owner.savedStateRegistry.consumeRestoredStateForKey(STATE_KEY)
        if (restoreBundle == null) {
            checkedItemCount = 0
            checkedItemPositions = SparseBooleanArray(0)
            checkedIdStates = LongSparseArray(0)
        } else {
            val savedState: SavedState = restoreBundle.getParcelableCompat(PARCELABLE_KEY)!!
            checkedItemCount = savedState.checkedItemCount
            checkedItemPositions = savedState.checkedItemPositions
            checkedIdStates = savedState.checkedIdStates
        }
        owner.savedStateRegistry.registerSavedStateProvider(STATE_KEY) {
            Bundle(1).apply {
                putParcelable(PARCELABLE_KEY, SavedState(checkedItemCount, checkedItemPositions.clone(), checkedIdStates.clone()))
            }
        }
    }

    private val adapterDataSetObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            confirmCheckedPositions()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            confirmCheckedPositions()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            confirmCheckedPositions()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            confirmCheckedPositions()
        }
    }

    private val adapterLifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            adapter = null
            adapterLifecycle = null
            choiceActionMode?.finish()
        }
    }

    fun setAdapter(adapter: RecyclerView.Adapter<*>, adapterLifecycleOwner: LifecycleOwner) {
        if (this.adapter !== adapter) {
            this.adapter?.unregisterAdapterDataObserver(adapterDataSetObserver)
            this.adapterLifecycle?.removeObserver(adapterLifecycleObserver)
            this.adapter = adapter
            this.adapterLifecycle = adapterLifecycleOwner.lifecycle
            adapter.registerAdapterDataObserver(adapterDataSetObserver)
            adapterLifecycleOwner.lifecycle.addObserver(adapterLifecycleObserver)
            if (!adapter.hasStableIds()) {
                checkedIdStates.clear()
            }
            confirmCheckedPositions()
            if (checkedItemCount > 0) {
                startSupportActionModeIfNeeded()
            }
        }
    }

    fun isItemChecked(position: Int): Boolean {
        return checkedItemPositions[position]
    }

    val checkedItemIds: LongArray
        get() {
            val idStates = checkedIdStates
            return LongArray(idStates.size()) { idStates.keyAt(it) }
        }

    fun clearChoices() {
        if (checkedItemCount > 0) {
            val start = checkedItemPositions.keyAt(0)
            val end = checkedItemPositions.keyAt(checkedItemPositions.size() - 1)
            checkedItemPositions.clear()
            checkedIdStates.clear()
            checkedItemCount = 0

            adapter?.notifyItemRangeChanged(start, end - start + 1, SELECTION_PAYLOAD)

            choiceActionMode?.finish()
        }
    }

    fun setItemChecked(position: Int, value: Boolean) {
        // Start selection mode if needed. We don't need it if we're unchecking something.
        if (value) {
            startSupportActionModeIfNeeded()
        }

        val oldValue = checkedItemPositions[position]
        checkedItemPositions[position] = value

        if (oldValue != value) {
            val id = adapter?.getItemId(position) ?: RecyclerView.NO_ID

            if (adapter?.hasStableIds() == true) {
                if (value) {
                    checkedIdStates[id] = position
                } else {
                    checkedIdStates.remove(id)
                }
            }

            if (value) {
                checkedItemCount++
            } else {
                checkedItemCount--
            }

            adapter?.notifyItemChanged(position, SELECTION_PAYLOAD)

            choiceActionMode?.let {
                multiChoiceModeCallback.onItemCheckedStateChanged(it, position, id, value)
                if (checkedItemCount == 0) {
                    it.finish()
                }
            }
        }
    }

    fun toggleItemChecked(position: Int) {
        setItemChecked(position, !isItemChecked(position))
    }

    private fun startSupportActionModeIfNeeded() {
        if (choiceActionMode == null) {
            choiceActionMode = activity.startSupportActionMode(multiChoiceModeCallback)
        }
    }

    fun confirmCheckedPositions() {
        val adapter = this.adapter ?: return
        if (checkedItemCount == 0) {
            return
        }

        val itemCount = adapter.itemCount
        var checkedCountChanged = false

        if (itemCount == 0) {
            // Optimized path for empty adapter: remove all items.
            checkedItemPositions.clear()
            checkedIdStates.clear()
            checkedItemCount = 0
            checkedCountChanged = true
        } else if (adapter.hasStableIds()) {
            // Clear out the positional check states, we'll rebuild it below from IDs.
            checkedItemPositions.clear()

            var checkedIndex = 0
            while (checkedIndex < checkedIdStates.size) {
                val id = checkedIdStates.keyAt(checkedIndex)
                val lastPos = checkedIdStates.valueAt(checkedIndex)

                if (lastPos >= itemCount || id != adapter.getItemId(lastPos)) {
                    // Look around to see if the ID is nearby. If not, uncheck it.
                    val start = (lastPos - CHECK_POSITION_SEARCH_DISTANCE).coerceAtLeast(0)
                    val end = (lastPos + CHECK_POSITION_SEARCH_DISTANCE).coerceAtMost(itemCount)
                    var found = false
                    for (searchPos in start until end) {
                        val searchId = adapter.getItemId(searchPos)
                        if (id == searchId) {
                            found = true
                            checkedItemPositions[searchPos] = true
                            checkedIdStates.setValueAt(checkedIndex, searchPos)
                            break
                        }
                    }

                    if (!found) {
                        checkedIdStates.remove(id)
                        checkedIndex--
                        checkedItemCount--
                        checkedCountChanged = true
                        choiceActionMode?.let { multiChoiceModeCallback.onItemCheckedStateChanged(it, lastPos, id, false) }
                    }
                } else {
                    checkedItemPositions[lastPos] = true
                }
                checkedIndex++
            }
        } else {
            // If the total number of items decreased, remove all out-of-range check indexes.
            for (i in checkedItemPositions.size - 1 downTo 0) {
                val position = checkedItemPositions.keyAt(i)
                if (position < itemCount) {
                    break
                }
                if (checkedItemPositions.valueAt(i)) {
                    checkedItemCount--
                    checkedCountChanged = true
                }
                checkedItemPositions.delete(position)
            }
        }

        if (checkedCountChanged) {
            if (checkedItemCount == 0) {
                choiceActionMode?.finish()
            } else {
                choiceActionMode?.invalidate()
            }
        }
    }

    class SavedState(val checkedItemCount: Int,
                     val checkedItemPositions: SparseBooleanArray,
                     val checkedIdStates: LongSparseArray<Int>) : Parcelable {

        override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
            writeInt(checkedItemCount)
            writeSparseBooleanArray(checkedItemPositions)
            writeInt(checkedIdStates.size)
            checkedIdStates.forEach { key, value ->
                writeLong(key)
                writeInt(value)
            }
        }

        override fun describeContents() = 0

        companion object {
            @JvmField
            @Suppress("UNUSED")
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = with(source) {
                    val checkedItemCount = readInt()
                    val checkedItemPositions = readSparseBooleanArray()!!
                    val size = readInt()
                    val checkedIdStates = LongSparseArray<Int>(size).apply {
                        for (i in 0 until size) {
                            val key = readLong()
                            val value = readInt()
                            append(key, value)
                        }
                    }
                    SavedState(checkedItemCount, checkedItemPositions, checkedIdStates)
                }

                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }
    }

    private inner class MultiChoiceModeWrapper(private val wrapped: MultiChoiceModeListener) : MultiChoiceModeListener by wrapped {

        override fun onDestroyActionMode(mode: ActionMode) {
            wrapped.onDestroyActionMode(mode)
            choiceActionMode = null
            clearChoices()
        }
    }

    companion object {
        @JvmField
        val SELECTION_PAYLOAD = Any()

        private const val STATE_KEY = "MultiChoiceHelper"
        private const val PARCELABLE_KEY = "saved_state"
        private const val CHECK_POSITION_SEARCH_DISTANCE = 20
    }
}