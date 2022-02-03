package be.digitalia.fosdem.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.collection.SimpleArrayMap
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.core.view.isGone
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.EventDetailsActivity
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.utils.DateUtils
import be.digitalia.fosdem.widgets.MultiChoiceHelper
import java.time.format.DateTimeFormatter

class BookmarksAdapter(context: Context, private val multiChoiceHelper: MultiChoiceHelper) :
    ListAdapter<Event, BookmarksAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val timeFormatter = DateUtils.getTimeFormatter(context)

    @ColorInt
    private val errorColor: Int
    private val observers = SimpleArrayMap<AdapterDataObserver, BookmarksDataObserverWrapper>()

    var roomStatuses: Map<String, RoomStatus> = emptyMap()
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount, DETAILS_PAYLOAD)
            }
        }

    init {
        setHasStableIds(true)
        with(context.theme.obtainStyledAttributes(R.styleable.ErrorColors)) {
            errorColor = getColor(R.styleable.ErrorColors_colorError, 0)
            recycle()
        }
    }

    override fun getItemId(position: Int) = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return ViewHolder(view, multiChoiceHelper, timeFormatter, errorColor)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
        val previous = if (position > 0) getItem(position - 1) else null
        val next = if (position + 1 < itemCount) getItem(position + 1) else null
        holder.bindDetails(event, previous, next, roomStatuses[event.roomName])
        holder.bindSelection()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val event = getItem(position)
            if (DETAILS_PAYLOAD in payloads) {
                val previous = if (position > 0) getItem(position - 1) else null
                val next = if (position + 1 < itemCount) getItem(position + 1) else null
                holder.bindDetails(event, previous, next, roomStatuses[event.roomName])
            }
            if (MultiChoiceHelper.SELECTION_PAYLOAD in payloads) {
                holder.bindSelection()
            }
        }
    }

    override fun registerAdapterDataObserver(observer: AdapterDataObserver) {
        if (!observers.containsKey(observer)) {
            val wrapper = BookmarksDataObserverWrapper(observer, this)
            observers.put(observer, wrapper)
            super.registerAdapterDataObserver(wrapper)
        }
    }

    override fun unregisterAdapterDataObserver(observer: AdapterDataObserver) {
        val wrapper = observers.remove(observer)
        if (wrapper != null) {
            super.unregisterAdapterDataObserver(wrapper)
        }
    }

    class ViewHolder(itemView: View, helper: MultiChoiceHelper,
                     private val timeFormatter: DateTimeFormatter, @ColorInt private val errorColor: Int)
        : MultiChoiceHelper.ViewHolder(itemView, helper), View.OnClickListener {
        private val title: TextView = itemView.findViewById(R.id.title)
        private val persons: TextView = itemView.findViewById(R.id.persons)
        private val trackName: TextView = itemView.findViewById(R.id.track_name)
        private val details: TextView = itemView.findViewById(R.id.details)

        private var event: Event? = null

        init {
            setOnClickListener(this)
        }

        fun bind(event: Event) {
            val context = itemView.context
            this.event = event

            title.text = event.title
            val personsSummary = event.personsSummary
            persons.text = personsSummary
            persons.isGone = personsSummary.isNullOrEmpty()
            val track = event.track
            trackName.text = track.name
            trackName.setTextColor(ContextCompat.getColorStateList(context, track.type.textColorResId))
            trackName.contentDescription = context.getString(R.string.track_content_description, track.name)
        }

        fun bindDetails(event: Event, previous: Event?, next: Event?, roomStatus: RoomStatus?) {
            val context = details.context
            val startTimeString = event.startTime?.atZone(DateUtils.conferenceZoneId)?.format(timeFormatter) ?: "?"
            val endTimeString = event.endTime?.atZone(DateUtils.conferenceZoneId)?.format(timeFormatter) ?: "?"
            val roomName = event.roomName.orEmpty()
            val detailsText: CharSequence = "${event.day.shortName}, $startTimeString ― $endTimeString  |  $roomName"
            val detailsSpannable = SpannableString(detailsText)
            var detailsDescription = detailsText

            // Highlight the date and time with error color in case of conflicting schedules
            if (isOverlapping(event, previous, next)) {
                val endPosition = detailsText.indexOf(" | ")
                detailsSpannable[0, endPosition] = ForegroundColorSpan(errorColor)
                detailsSpannable[0, endPosition] = StyleSpan(Typeface.BOLD)
                detailsDescription = context.getString(R.string.bookmark_conflict_content_description, detailsDescription)
            }
            if (roomStatus != null) {
                val color = ContextCompat.getColor(context, roomStatus.colorResId)
                detailsSpannable[detailsText.length - roomName.length, detailsText.length] = ForegroundColorSpan(color)
            }
            details.text = detailsSpannable
            details.contentDescription = context.getString(R.string.details_content_description, detailsDescription)
        }

        /**
         * Checks if the current event is overlapping with the previous or next one.
         */
        private fun isOverlapping(event: Event, previous: Event?, next: Event?): Boolean {
            val startTime = event.startTime
            val previousEndTime = previous?.endTime
            if (startTime != null && previousEndTime != null && previousEndTime > startTime) {
                // The event overlaps with the previous one
                return true
            }
            val endTime = event.endTime
            val nextStartTime = next?.startTime
            // The event overlaps with the next one
            return endTime != null && nextStartTime != null && nextStartTime < endTime
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

    /**
     * An observer dispatching updates to the source observer while additionally notifying changes
     * of the immediately previous and next items in order to properly update their overlapping status display.
     */
    private class BookmarksDataObserverWrapper(private val observer: AdapterDataObserver, private val adapter: RecyclerView.Adapter<*>)
        : AdapterDataObserver() {

        private fun updatePrevious(position: Int) {
            if (position >= 0) {
                observer.onItemRangeChanged(position, 1, DETAILS_PAYLOAD)
            }
        }

        private fun updateNext(position: Int) {
            if (position < adapter.itemCount) {
                observer.onItemRangeChanged(position, 1, DETAILS_PAYLOAD)
            }
        }

        override fun onChanged() {
            observer.onChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            observer.onItemRangeChanged(positionStart, itemCount)
            updatePrevious(positionStart - 1)
            updateNext(positionStart + itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            observer.onItemRangeChanged(positionStart, itemCount, payload)
            updatePrevious(positionStart - 1)
            updateNext(positionStart + itemCount)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            observer.onItemRangeInserted(positionStart, itemCount)
            updatePrevious(positionStart - 1)
            updateNext(positionStart + itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            observer.onItemRangeRemoved(positionStart, itemCount)
            updatePrevious(positionStart - 1)
            updateNext(positionStart)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            updatePrevious(fromPosition - 1)
            updateNext(fromPosition + itemCount)
            observer.onItemRangeMoved(fromPosition, toPosition, itemCount)
            updatePrevious(toPosition - 1)
            updateNext(toPosition + itemCount)
        }
    }

    companion object {
        private val DIFF_CALLBACK = createSimpleItemCallback<Event> { oldItem, newItem ->
            oldItem.id == newItem.id
        }
        private val DETAILS_PAYLOAD = Any()
    }
}