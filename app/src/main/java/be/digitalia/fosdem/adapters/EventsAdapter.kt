package be.digitalia.fosdem.adapters

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.core.view.isGone
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.EventDetailsActivity
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.model.StatusEvent
import be.digitalia.fosdem.utils.DateUtils
import java.time.format.DateTimeFormatter

class EventsAdapter constructor(context: Context, private val showDay: Boolean = true) :
    PagedListAdapter<StatusEvent, EventsAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val timeFormatter = DateUtils.getTimeFormatter(context)

    var roomStatuses: Map<String, RoomStatus> = emptyMap()
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount, DETAILS_PAYLOAD)
            }
        }

    override fun getItemViewType(position: Int) = R.layout.item_event

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return ViewHolder(view, timeFormatter)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val statusEvent = getItem(position)
        if (statusEvent == null) {
            holder.clear()
        } else {
            val event = statusEvent.event
            holder.bind(event, statusEvent.isBookmarked)
            holder.bindDetails(event, showDay, roomStatuses[event.roomName])
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val statusEvent = getItem(position)
            if (statusEvent != null) {
                if (DETAILS_PAYLOAD in payloads) {
                    val event = statusEvent.event
                    holder.bindDetails(event, showDay, roomStatuses[event.roomName])
                }
            }
        }
    }

    class ViewHolder(itemView: View, private val timeFormatter: DateTimeFormatter)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val title: TextView = itemView.findViewById(R.id.title)
        private val persons: TextView = itemView.findViewById(R.id.persons)
        private val trackName: TextView = itemView.findViewById(R.id.track_name)
        private val details: TextView = itemView.findViewById(R.id.details)

        private var event: Event? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun clear() {
            event = null
            title.text = null
            persons.text = null
            trackName.text = null
            details.text = null
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
            val track = event.track
            trackName.text = track.name
            trackName.setTextColor(ContextCompat.getColorStateList(context, track.type.textColorResId))
            trackName.contentDescription = context.getString(R.string.track_content_description, track.name)
        }

        fun bindDetails(event: Event, showDay: Boolean, roomStatus: RoomStatus?) {
            val context = details.context
            val startTimeString = event.startTime?.atZone(DateUtils.conferenceZoneId)?.format(timeFormatter) ?: "?"
            val endTimeString = event.endTime?.atZone(DateUtils.conferenceZoneId)?.format(timeFormatter) ?: "?"
            val roomName = event.roomName.orEmpty()
            var detailsText: CharSequence = if (showDay) {
                "${event.day.shortName}, $startTimeString ― $endTimeString  |  $roomName"
            } else {
                "$startTimeString ― $endTimeString  |  $roomName"
            }
            var detailsDescription = detailsText
            if (roomStatus != null) {
                val color = ContextCompat.getColor(context, roomStatus.colorResId)
                detailsText = SpannableString(detailsText).apply {
                    this[detailsText.length - roomName.length, detailsText.length] = ForegroundColorSpan(color)
                }
                detailsDescription = "$detailsDescription (${context.getString(roomStatus.nameResId)})"
            }
            details.text = detailsText
            details.contentDescription = context.getString(R.string.details_content_description, detailsDescription)
        }

        override fun onClick(view: View) {
            event?.let {
                val context = view.context
                val intent = Intent(context, EventDetailsActivity::class.java)
                        .putExtra(EventDetailsActivity.EXTRA_EVENT, it)
                context.startActivity(intent)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = createSimpleItemCallback<StatusEvent> { oldItem, newItem ->
            oldItem.event.id == newItem.event.id
        }
        private val DETAILS_PAYLOAD = Any()
    }
}