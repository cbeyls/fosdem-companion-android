package be.digitalia.fosdem.adapters

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
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
import be.digitalia.fosdem.utils.atZoneOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TrackScheduleAdapter(context: Context, private val clickListener: (Event) -> Unit)
    : ListAdapter<StatusEvent, TrackScheduleAdapter.ViewHolder>(EventsAdapter.DIFF_CALLBACK) {

    private val timeFormatter = DateUtils.getTimeFormatter(context)
    private val viewHolderResources: ViewHolderResources

    init {
        setHasStableIds(true)

        val typedArray = context.theme.obtainStyledAttributes(R.styleable.PrimaryTextColors)
        viewHolderResources = ViewHolderResources(
            activatedBackgroundResId = R.drawable.activated_background,
            timeBackgroundColor = ContextCompat.getColor(context, R.color.schedule_time_background),
            timeRunningBackgroundColor = ContextCompat.getColor(context, R.color.schedule_time_running_background),
            timeForegroundColor = typedArray.getColor(R.styleable.PrimaryTextColors_android_textColorPrimary, 0),
            timeRunningForegroundColor = typedArray.getColor(R.styleable.PrimaryTextColors_android_textColorPrimaryInverse, 0)
        )
        typedArray.recycle()
    }

    var zoneId: ZoneId? = null
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount, TIME_PAYLOAD)
            }
        }

    var currentTime: Instant? = null
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount, TIME_COLORS_PAYLOAD)
            }
        }

    var selectedId: Long = RecyclerView.NO_ID
        set(value) {
            val oldId = field
            if (oldId != value) {
                field = value
                for (i in 0 until itemCount) {
                    val id = getItemId(i)
                    if (id == oldId || id == value) {
                        notifyItemChanged(i, SELECTION_PAYLOAD)
                    }
                }
            }
        }

    override fun getItemId(position: Int) = getItem(position).event.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_event, parent, false)
        return ViewHolder(view, timeFormatter, viewHolderResources, clickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val statusEvent = getItem(position)
        val event = statusEvent.event
        holder.bind(event, statusEvent.isBookmarked)
        holder.bindTime(event, zoneId)
        holder.bindTimeColors(event, currentTime)
        holder.bindSelection(event.id == selectedId)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val statusEvent = getItem(position)
            if (TIME_PAYLOAD in payloads) {
                holder.bindTime(statusEvent.event, zoneId)
            }
            if (TIME_COLORS_PAYLOAD in payloads) {
                holder.bindTimeColors(statusEvent.event, currentTime)
            }
            if (SELECTION_PAYLOAD in payloads) {
                holder.bindSelection(statusEvent.event.id == selectedId)
            }
        }
    }

    class ViewHolderResources(
        @DrawableRes val activatedBackgroundResId: Int,
        @ColorInt val timeBackgroundColor: Int,
        @ColorInt val timeRunningBackgroundColor: Int,
        @ColorInt val timeForegroundColor: Int,
        @ColorInt val timeRunningForegroundColor: Int
    )

    class ViewHolder(
        itemView: View, private val timeFormatter: DateTimeFormatter,
        private val resources: ViewHolderResources, private val clickListener: (Event) -> Unit
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val time: TextView = itemView.findViewById(R.id.time)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val persons: TextView = itemView.findViewById(R.id.persons)
        private val room: TextView = itemView.findViewById(R.id.room)

        private var event: Event? = null

        init {
            itemView.setOnClickListener(this)
            if (resources.activatedBackgroundResId != 0) {
                // Compose a new background drawable by combining the existing one with the activated background
                val existingBackground = itemView.background
                val activatedBackground =
                    ContextCompat.getDrawable(itemView.context, resources.activatedBackgroundResId)
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

        fun bindTime(event: Event, zoneId: ZoneId?) {
            time.text = event.startTime?.atZoneOrNull(zoneId)?.format(timeFormatter)
        }

        fun bindTimeColors(event: Event, currentTime: Instant?) {
            if (currentTime != null && event.isRunningAtTime(currentTime)) {
                // Contrast colors for running event
                time.setBackgroundColor(resources.timeRunningBackgroundColor)
                time.setTextColor(resources.timeRunningForegroundColor)
                time.contentDescription = time.context.getString(R.string.in_progress_content_description, time.text)
            } else {
                // Normal colors
                time.setBackgroundColor(resources.timeBackgroundColor)
                time.setTextColor(resources.timeForegroundColor)
                // Use text as content description
                time.contentDescription = null
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
        private val TIME_PAYLOAD = Any()
        private val TIME_COLORS_PAYLOAD = Any()
        private val SELECTION_PAYLOAD = Any()
    }
}