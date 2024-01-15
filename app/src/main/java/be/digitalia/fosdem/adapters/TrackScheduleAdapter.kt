package be.digitalia.fosdem.adapters

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.utils.DateUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TrackScheduleAdapter(context: Context, private val clickListener: (Event) -> Unit)
    : ListAdapter<StatusEvent, TrackScheduleAdapter.ViewHolder>(EventsAdapter.DIFF_CALLBACK) {

    private val timeFormatter = DateUtils.getTimeFormatter(context)

    var timeZoneOverride: ZoneId? = null
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount, TIME_PAYLOAD)
            }
        }

    var currentTime: Instant? = null
        set(value) {
            val oldTime = field
            if (field != value) {
                field = value
                val list = currentList
                val oldIndex = getOngoingIndexForTime(list, oldTime)
                val newIndex = getOngoingIndexForTime(list, value)
                if (oldIndex != newIndex) {
                    if (oldIndex >= 0) notifyItemChanged(oldIndex)
                    if (newIndex >= 0) notifyItemChanged(newIndex)
                }
            }
        }

    private fun getOngoingIndexForTime(list: List<StatusEvent>, time: Instant?): Int {
        if (time == null) {
            return -1
        }
        var index = list.binarySearchBy(time) { it.event.startTime }
        if (index >= 0) {
            return index
        }
        // Check if the event at previous position is in progress
        index = -index - 2
        return if (index >= 0 && list[index].event.isRunningAtTime(time)) index else -1
    }

    var selectedId: Long = RecyclerView.NO_ID
        set(value) {
            val oldId = field
            if (oldId != value) {
                field = value
                currentList.forEachIndexed { index, statusEvent ->
                    val id = statusEvent.event.id
                    if (id == oldId || id == value) {
                        notifyItemChanged(index, SELECTION_PAYLOAD)
                    }
                }
            }
        }

    override fun getItemViewType(position: Int): Int {
        val time = currentTime
        return if (time != null && getItem(position).event.isRunningAtTime(time))
            ONGOING_VIEW_TYPE else DEFAULT_VIEW_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = if (viewType == ONGOING_VIEW_TYPE)
            ContextThemeWrapper(parent.context, R.style.ThemeOverlay_App_OngoingEvent)
        else parent.context
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_schedule_event, parent, false)
        return ViewHolder(view, timeFormatter, R.drawable.activated_background, clickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val statusEvent = getItem(position)
        val event = statusEvent.event
        holder.bind(event, statusEvent.isBookmarked)
        holder.bindTime(event, timeZoneOverride)
        holder.bindSelection(event.id == selectedId)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val statusEvent = getItem(position)
            if (TIME_PAYLOAD in payloads) {
                holder.bindTime(statusEvent.event, timeZoneOverride)
            }
            if (SELECTION_PAYLOAD in payloads) {
                holder.bindSelection(statusEvent.event.id == selectedId)
            }
        }
    }

    class ViewHolder(
        itemView: View, private val timeFormatter: DateTimeFormatter,
        @DrawableRes private val activatedBackgroundResId: Int, private val clickListener: (Event) -> Unit
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val time: TextView = itemView.findViewById(R.id.time)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val persons: TextView = itemView.findViewById(R.id.persons)
        private val room: TextView = itemView.findViewById(R.id.room)

        private var event: Event? = null

        init {
            itemView.setOnClickListener(this)
            if (activatedBackgroundResId != 0) {
                // Compose a new background drawable by combining the existing one with the activated background
                val existingBackground = itemView.background
                val activatedBackground =
                    ContextCompat.getDrawable(itemView.context, activatedBackgroundResId)
                val newBackground = if (existingBackground == null) {
                    activatedBackground
                } else {
                    // Clear the existing background drawable callback so it can be assigned to the LayerDrawable
                    itemView.background = null
                    LayerDrawable(arrayOf(existingBackground, activatedBackground))
                }
                itemView.background = newBackground
            }
        }

        fun bind(event: Event, isBookmarked: Boolean) {
            val context = itemView.context
            this.event = event

            title.text = event.title
            val bookmarkDrawable = if (isBookmarked) AppCompatResources.getDrawable(context, R.drawable.ic_bookmark_white_24dp) else null
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, bookmarkDrawable, null)
            title.contentDescription = if (isBookmarked) {
                context.getString(R.string.in_bookmarks_content_description, event.title.orEmpty())
            } else null
            val personsSummary = event.personsSummary
            persons.text = personsSummary
            persons.isGone = personsSummary.isNullOrEmpty()
            room.text = event.roomName
            room.contentDescription = context.getString(R.string.room_content_description, event.roomName.orEmpty())
        }

        fun bindTime(event: Event, timeZoneOverride: ZoneId?) {
            time.text = event.startTime(timeZoneOverride)?.format(timeFormatter) ?: "?"
            if (itemViewType == ONGOING_VIEW_TYPE) {
                time.contentDescription =
                    time.context.getString(R.string.in_progress_content_description, time.text)
            }
        }

        fun bindSelection(isSelected: Boolean) {
            itemView.isActivated = isSelected
        }

        override fun onClick(v: View) {
            event?.let { clickListener(it) }
        }
    }

    companion object {
        private const val DEFAULT_VIEW_TYPE = 0
        private const val ONGOING_VIEW_TYPE = 1
        private val TIME_PAYLOAD = Any()
        private val SELECTION_PAYLOAD = Any()
    }
}