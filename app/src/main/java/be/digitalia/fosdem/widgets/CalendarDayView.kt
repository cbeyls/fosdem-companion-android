package be.digitalia.fosdem.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import be.digitalia.fosdem.R
import be.digitalia.fosdem.model.Event
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.max
import kotlin.math.min

/**
 * Custom View that displays events in a day-view calendar style.
 * Events are positioned by start/end time and arranged horizontally when overlapping.
 */
class CalendarDayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {
        val DEFAULT_COLOR = Color.rgb(100, 100, 100)
        const val DEFAULT_START_HOUR = 8
        const val DEFAULT_END_HOUR = 24
        const val PADDING_MINUTES = 30
        const val PAST_EVENT_ALPHA = 100 // out of 255
    }

    private data class EventLayout(
        val event: Event,
        val rect: RectF,
        val backgroundColor: Int,
        val isPast: Boolean
    )

    var currentTime: Instant? = null
        set(value) {
            if (field != value) {
                field = value
                layoutEvents()
                invalidate()
            }
        }

    var timeZoneOverride: ZoneId? = null
        set(value) {
            if (field != value) {
                field = value
                layoutEvents()
                invalidate()
            }
        }

    var events: List<Event> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                layoutEvents()
                requestLayout()
                invalidate()
            }
        }

    var onEventClickListener: ((Event) -> Unit)? = null

    /**
     * Function to get color for a room name. Should return a consistent color for the same room.
     */
    var roomColorProvider: ((String) -> Int)? = null
        set(value) {
            if (field != value) {
                field = value
                layoutEvents()
                invalidate()
            }
        }

    private val hourHeight: Float
    private val timeColumnWidth: Float
    private val eventPadding: Float
    private val eventCornerRadius: Float
    private val eventMinHeight: Float

    // Dynamic time range based on events (with 30 min padding, rounded to hours)
    private var startHour = DEFAULT_START_HOUR
    private var endHour = DEFAULT_END_HOUR
    private val totalHours: Int
        get() = endHour - startHour

    private val gridPaint: Paint
    private val hourTextPaint: Paint
    private val eventBorderPaint: Paint
    private val currentTimeLinePaint: Paint
    private val currentTimeCirclePaint: Paint
    private val currentTimeCircleRadius: Float

    init {
        // Get theme colors
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        val textColorSecondary = ContextCompat.getColor(context, typedValue.resourceId)

        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val textColorPrimary = ContextCompat.getColor(context, typedValue.resourceId)

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (textColorSecondary and 0x00FFFFFF) or 0x40000000  // Use theme color with alpha
            strokeWidth = 1f * resources.displayMetrics.density
        }

        hourTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorSecondary
            textSize = 12f * resources.displayMetrics.scaledDensity
            textAlign = Paint.Align.RIGHT
        }

        eventBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = (textColorPrimary and 0x00FFFFFF) or 0x60000000  // Theme color with alpha
            strokeWidth = 1f * resources.displayMetrics.density
        }

        val gridColor = gridPaint.color
        val density = resources.displayMetrics.density
        currentTimeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gridColor
            strokeWidth = 2f * density
        }
        currentTimeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gridColor
            style = Paint.Style.FILL
        }
        currentTimeCircleRadius = 4f * density

        hourHeight = resources.getDimension(R.dimen.calendar_hour_height)
        timeColumnWidth = resources.getDimension(R.dimen.calendar_time_column_width)
        eventPadding = resources.getDimension(R.dimen.calendar_event_padding)
        eventCornerRadius = resources.getDimension(R.dimen.calendar_event_corner_radius)
        eventMinHeight = resources.getDimension(R.dimen.calendar_event_min_height)
    }

    private val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val eventTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 12f * resources.displayMetrics.scaledDensity
        typeface = Typeface.DEFAULT_BOLD
    }

    private val eventRoomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xDDFFFFFF.toInt()
        textSize = 11f * resources.displayMetrics.scaledDensity
    }

    private var eventLayouts: List<EventLayout> = emptyList()
    private var hourLabels: Array<String> = emptyArray()

    private val touchHelper = CalendarTouchHelper()

    init {
        ViewCompat.setAccessibilityDelegate(this, touchHelper)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (totalHours * hourHeight + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(width, height)
    }

    private fun layoutEvents() {
        val availableWidth = width - timeColumnWidth - paddingLeft - paddingRight
        if (availableWidth <= 0 || events.isEmpty()) {
            eventLayouts = emptyList()
            startHour = DEFAULT_START_HOUR
            endHour = DEFAULT_END_HOUR
            return
        }

        // Calculate time range based on events (30 min padding, rounded to hours)
        var minMinutes = Int.MAX_VALUE
        var maxMinutes = Int.MIN_VALUE
        for (event in events) {
            val start = event.startTime(timeZoneOverride)?.toLocalTime() ?: continue
            val end = event.endTime(timeZoneOverride)?.toLocalTime() ?: continue
            val startMins = start.hour * 60 + start.minute
            val endMins = end.hour * 60 + end.minute
            minMinutes = min(minMinutes, startMins)
            maxMinutes = max(maxMinutes, endMins)
        }

        // Apply 30 minute padding and round to hours
        startHour = max(0, (minMinutes - PADDING_MINUTES) / 60)
        endHour = min(24, (maxMinutes + PADDING_MINUTES + 59) / 60)  // Round up

        // Pre-compute hour label strings to avoid allocations in onDraw
        val hourFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        hourLabels = Array(endHour - startHour + 1) { i ->
            LocalTime.of(startHour + i, 0).format(hourFormatter)
        }

        // Group overlapping events into clusters (transitive time overlap).
        // When an event bridges multiple clusters, merge them.
        val sortedEvents = events.filter { it.startTime != null && it.endTime != null }
            .sortedBy { it.startTime }
        val clusters = mutableListOf<MutableList<Event>>()

        for (event in sortedEvents) {
            val eventStart = event.startTime!!
            val eventEnd = event.endTime!!

            val overlappingClusters = clusters.filter { cluster ->
                cluster.any { other ->
                    eventStart < other.endTime!! && eventEnd > other.startTime!!
                }
            }

            if (overlappingClusters.isEmpty()) {
                clusters.add(mutableListOf(event))
            } else {
                // Merge all overlapping clusters into the first one
                val target = overlappingClusters.first()
                target.add(event)
                for (i in 1 until overlappingClusters.size) {
                    target.addAll(overlappingClusters[i])
                    clusters.remove(overlappingClusters[i])
                }
            }
        }

        // Layout each cluster using greedy room coloring for minimum columns
        val layouts = mutableListOf<EventLayout>()
        for (cluster in clusters) {
            val roomEvents = cluster.groupBy { it.roomName ?: "" }
            val rooms = roomEvents.keys.sorted()

            // Build room conflict graph: two rooms conflict if any of their events overlap
            val roomConflicts = rooms.associateWith { mutableSetOf<String>() }
            for (i in rooms.indices) {
                for (j in i + 1 until rooms.size) {
                    val roomA = rooms[i]
                    val roomB = rooms[j]
                    val conflicts = roomEvents[roomA]!!.any { a ->
                        roomEvents[roomB]!!.any { b ->
                            a.startTime!! < b.endTime!! && a.endTime!! > b.startTime!!
                        }
                    }
                    if (conflicts) {
                        roomConflicts[roomA]!!.add(roomB)
                        roomConflicts[roomB]!!.add(roomA)
                    }
                }
            }

            // Greedy coloring: assign each room the smallest column not used by conflicting rooms
            val roomToColumn = mutableMapOf<String, Int>()
            for (room in rooms) {
                val usedColumns = roomConflicts[room]!!.mapNotNullTo(mutableSetOf()) { roomToColumn[it] }
                var col = 0
                while (col in usedColumns) col++
                roomToColumn[room] = col
            }

            val columnCount = (roomToColumn.values.maxOrNull() ?: 0) + 1
            val columnWidth = availableWidth / columnCount

            for (event in cluster) {
                val startTime = event.startTime(timeZoneOverride)?.toLocalTime() ?: continue
                val endTime = event.endTime(timeZoneOverride)?.toLocalTime() ?: continue

                val startMinutes = (startTime.hour - startHour) * 60 + startTime.minute
                val endMinutes = (endTime.hour - startHour) * 60 + endTime.minute

                val columnIndex = roomToColumn[event.roomName ?: ""] ?: 0
                val top = paddingTop + (startMinutes * hourHeight / 60f)
                val bottom = max(top + eventMinHeight, paddingTop + (endMinutes * hourHeight / 60f))
                val left = paddingLeft + timeColumnWidth + columnIndex * columnWidth + eventPadding
                val right = left + columnWidth - 2 * eventPadding

                val backgroundColor = roomColorProvider?.invoke(event.roomName ?: "") ?: DEFAULT_COLOR
                val isPast = currentTime != null && event.endTime != null && event.endTime <= currentTime
                layouts.add(EventLayout(event, RectF(left, top, right, bottom), backgroundColor, isPast))
            }
        }

        eventLayouts = layouts
        touchHelper.invalidateRoot()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutEvents()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawTimeGrid(canvas)
        drawEvents(canvas)
        drawCurrentTimeLine(canvas)
    }

    private fun drawTimeGrid(canvas: Canvas) {
        val leftMargin = paddingLeft + timeColumnWidth
        val rightEdge = width.toFloat() - paddingRight
        val labelX = paddingLeft + timeColumnWidth - 8 * resources.displayMetrics.density

        // Draw vertical separator line
        canvas.drawLine(leftMargin, paddingTop.toFloat(), leftMargin, paddingTop + totalHours * hourHeight, gridPaint)

        // Draw hour lines and labels
        for (i in 0..totalHours) {
            val y = paddingTop + i * hourHeight

            // Draw grid line
            canvas.drawLine(leftMargin, y, rightEdge, y, gridPaint)

            // Draw hour label (aligned to the hour line)
            if (i < hourLabels.size) {
                val textY = y + hourTextPaint.textSize / 3  // Vertically center on the line
                canvas.drawText(hourLabels[i], labelX, textY, hourTextPaint)
            }
        }
    }

    private fun drawEvents(canvas: Canvas) {
        for (layout in eventLayouts) {
            val rect = layout.rect
            val event = layout.event
            val alpha = if (layout.isPast) PAST_EVENT_ALPHA else 255

            // Draw event background
            eventPaint.color = layout.backgroundColor
            eventPaint.alpha = alpha
            canvas.drawRoundRect(rect, eventCornerRadius, eventCornerRadius, eventPaint)

            // Draw event border
            val savedBorderAlpha = eventBorderPaint.alpha
            eventBorderPaint.alpha = savedBorderAlpha * alpha / 255
            canvas.drawRoundRect(rect, eventCornerRadius, eventCornerRadius, eventBorderPaint)
            eventBorderPaint.alpha = savedBorderAlpha

            // Draw event content
            val textPadding = 4 * resources.displayMetrics.density
            val textX = rect.left + textPadding
            val availableTextWidth = rect.width() - 2 * textPadding

            // Draw title
            val savedTitleAlpha = eventTitlePaint.alpha
            eventTitlePaint.alpha = savedTitleAlpha * alpha / 255
            val titleY = rect.top + eventTitlePaint.textSize + textPadding
            val title = event.title ?: ""
            val ellipsizedTitle = ellipsizeText(title, eventTitlePaint, availableTextWidth)
            canvas.drawText(ellipsizedTitle, textX, min(titleY, rect.bottom - textPadding), eventTitlePaint)
            eventTitlePaint.alpha = savedTitleAlpha

            // Draw room name if there's space
            val roomY = titleY + eventRoomPaint.textSize + 2 * resources.displayMetrics.density
            if (roomY < rect.bottom - textPadding) {
                val savedRoomAlpha = eventRoomPaint.alpha
                eventRoomPaint.alpha = savedRoomAlpha * alpha / 255
                val roomName = event.roomName ?: ""
                val ellipsizedRoom = ellipsizeText(roomName, eventRoomPaint, availableTextWidth)
                canvas.drawText(ellipsizedRoom, textX, roomY, eventRoomPaint)
                eventRoomPaint.alpha = savedRoomAlpha
            }
        }
    }

    private fun drawCurrentTimeLine(canvas: Canvas) {
        val now = currentTime ?: return
        val zone = timeZoneOverride ?: ZoneId.systemDefault()
        val localTime = now.atZone(zone).toLocalTime()
        val minutes = (localTime.hour - startHour) * 60 + localTime.minute
        if (minutes < 0 || minutes > totalHours * 60) return

        val y = paddingTop + minutes * hourHeight / 60f
        val leftMargin = paddingLeft + timeColumnWidth
        val rightEdge = width.toFloat() - paddingRight

        canvas.drawCircle(leftMargin, y, currentTimeCircleRadius, currentTimeCirclePaint)
        canvas.drawLine(leftMargin, y, rightEdge, y, currentTimeLinePaint)
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) {
            return text
        }
        val ellipsis = "..."
        val ellipsisWidth = paint.measureText(ellipsis)
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + ellipsisWidth > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + ellipsis else ellipsis
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return touchHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touch is on an event - if so, claim the touch sequence
                val x = motionEvent.x
                val y = motionEvent.y
                for (layout in eventLayouts) {
                    if (layout.rect.contains(x, y)) {
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (onEventClickListener != null) {
                    val x = motionEvent.x
                    val y = motionEvent.y
                    for (layout in eventLayouts) {
                        if (layout.rect.contains(x, y)) {
                            performClick()
                            onEventClickListener?.invoke(layout.event)
                            return true
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(motionEvent)
    }

    private inner class CalendarTouchHelper : ExploreByTouchHelper(this@CalendarDayView) {

        private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        private val tempRect = Rect()

        override fun getVirtualViewAt(x: Float, y: Float): Int {
            for (i in eventLayouts.indices.reversed()) {
                if (eventLayouts[i].rect.contains(x, y)) {
                    return i
                }
            }
            return HOST_ID
        }

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            for (i in eventLayouts.indices) {
                virtualViewIds.add(i)
            }
        }

        override fun onPopulateNodeForVirtualView(
            virtualViewId: Int,
            node: AccessibilityNodeInfoCompat
        ) {
            val layout = eventLayouts[virtualViewId]
            val event = layout.event

            layout.rect.round(tempRect)
            node.setBoundsInParent(tempRect)

            val title = event.title ?: ""
            val room = event.roomName ?: ""
            val startTime = event.startTime(timeZoneOverride)?.toLocalTime()?.format(timeFormatter) ?: ""
            val endTime = event.endTime(timeZoneOverride)?.toLocalTime()?.format(timeFormatter) ?: ""
            node.contentDescription = "$title, $room, $startTime – $endTime"

            node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
        }

        override fun onPerformActionForVirtualView(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?
        ): Boolean {
            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
                val event = eventLayouts.getOrNull(virtualViewId)?.event ?: return false
                onEventClickListener?.invoke(event)
                return true
            }
            return false
        }

        override fun onPopulateEventForVirtualView(virtualViewId: Int, event: AccessibilityEvent) {
            val layout = eventLayouts.getOrNull(virtualViewId)
            event.contentDescription = layout?.event?.title ?: ""
        }
    }
}
