package be.digitalia.fosdem.fragments

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.set
import androidx.core.view.isVisible
import androidx.core.view.plusAssign
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.PersonInfoActivity
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.model.Building
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails
import be.digitalia.fosdem.model.Link
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.utils.ClickableArrowKeyMovementMethod
import be.digitalia.fosdem.utils.DateUtils
import be.digitalia.fosdem.utils.configureToolbarColors
import be.digitalia.fosdem.utils.parseHtml
import be.digitalia.fosdem.utils.roomNameToResourceName
import be.digitalia.fosdem.utils.stripHtml
import be.digitalia.fosdem.viewmodels.EventDetailsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EventDetailsFragment : Fragment(R.layout.fragment_event_details) {

    private class ViewHolder(view: View) {
        val personsTextView: TextView = view.findViewById(R.id.persons)
        val roomStatusTextView: TextView = view.findViewById(R.id.room_status)
        val linksHeader: View = view.findViewById(R.id.links_header)
        val linksContainer: ViewGroup = view.findViewById(R.id.links_container)
    }

    @Inject
    lateinit var api: FosdemApi
    private val viewModel: EventDetailsViewModel by viewModels()

    val event by lazy<Event>(LazyThreadSafetyMode.NONE) {
        requireArguments().getParcelable(ARG_EVENT)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val holder = ViewHolder(view).apply {
            view.findViewById<TextView>(R.id.title).text = event.title
            view.findViewById<TextView>(R.id.subtitle).apply {
                val subTitle = event.subTitle
                if (subTitle.isNullOrEmpty()) {
                    isVisible = false
                } else {
                    text = subTitle
                }
            }

            personsTextView.apply {
                // Set the persons summary text first;
                // replace it with the clickable text when the event details loading completes
                val personsSummary = event.personsSummary
                if (personsSummary.isNullOrEmpty()) {
                    isVisible = false
                } else {
                    text = personsSummary
                    movementMethod = LinkMovementMethod.getInstance()
                    isVisible = true
                }
            }

            view.findViewById<TextView>(R.id.time).apply {
                val timeFormatter = DateUtils.getTimeFormatter(context)
                val startTime = event.startTime?.atZone(DateUtils.conferenceZoneId)?.format(timeFormatter) ?: "?"
                val endTime = event.endTime?.atZone(DateUtils.conferenceZoneId)?.format(timeFormatter) ?: "?"
                text = "${event.day}, $startTime ― $endTime"
                contentDescription = getString(R.string.time_content_description, text)
            }

            view.findViewById<TextView>(R.id.room).apply {
                val roomName = event.roomName
                if (roomName.isNullOrEmpty()) {
                    isVisible = false
                } else {
                    val building = Building.fromRoomName(roomName)
                    val roomText = SpannableString(if (building == null) roomName else getString(R.string.room_building, roomName, building))
                    val roomImageResId = resources.getIdentifier(roomNameToResourceName(roomName), "drawable", requireActivity().packageName)
                    // If the room image exists, make the room text clickable to display it
                    if (roomImageResId != 0) {
                        roomText[0, roomText.length] = object : ClickableSpan() {
                            override fun onClick(view: View) {
                                parentFragmentManager.commit(allowStateLoss = true) {
                                    add<RoomImageDialogFragment>(RoomImageDialogFragment.TAG,
                                            args = RoomImageDialogFragment.createArguments(roomName, roomImageResId))
                                }
                            }

                            override fun updateDrawState(ds: TextPaint) {
                                super.updateDrawState(ds)
                                ds.isUnderlineText = false
                            }
                        }
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                    text = roomText
                    contentDescription = getString(R.string.room_content_description, roomText)
                }
            }

            view.findViewById<TextView>(R.id.abstract_text).apply {
                val abstractText = event.abstractText
                if (abstractText.isNullOrEmpty()) {
                    isVisible = false
                } else {
                    text = abstractText.parseHtml(resources)
                    movementMethod = ClickableArrowKeyMovementMethod
                }
            }

            view.findViewById<TextView>(R.id.description).apply {
                val descriptionText = event.description
                if (descriptionText.isNullOrEmpty()) {
                    isVisible = false
                } else {
                    text = descriptionText.parseHtml(resources)
                    movementMethod = ClickableArrowKeyMovementMethod
                }
            }
        }

        with(viewModel) {
            setEvent(event)
            eventDetails.observe(viewLifecycleOwner) { eventDetails ->
                showEventDetails(holder, eventDetails)
            }
        }

        // Live room status
        val roomName = event.roomName
        if (!roomName.isNullOrEmpty()) {
            holder.roomStatusTextView.run {
                api.roomStatuses.observe(viewLifecycleOwner) { roomStatuses ->
                    val roomStatus = roomStatuses[roomName]
                    if (roomStatus == null) {
                        text = null
                    } else {
                        setText(roomStatus.nameResId)
                        setTextColor(ContextCompat.getColorStateList(context, roomStatus.colorResId))
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.event, menu)
        menu.findItem(R.id.share)?.intent = createShareChooserIntent()
    }

    private fun createShareChooserIntent(): Intent {
        val title = event.title.orEmpty()
        val url = event.url.orEmpty()
        return ShareCompat.IntentBuilder(requireContext())
                .setSubject("$title ($CONFERENCE_NAME)")
                .setType("text/plain")
                .setText("$title $url $CONFERENCE_HASHTAG")
                .setChooserTitle(R.string.share)
                .createChooserIntent()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_to_agenda -> {
            addToAgenda()
            true
        }
        else -> false
    }

    private fun addToAgenda() {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            type = "vnd.android.cursor.item/event"
            event.title?.let { putExtra(CalendarContract.Events.TITLE, it) }
            val roomName = event.roomName
            val location = if (roomName.isNullOrEmpty()) VENUE_NAME else "$VENUE_NAME - $roomName"
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)

            var description = event.abstractText
            if (description.isNullOrEmpty()) {
                description = event.description.orEmpty()
            }
            description = description.stripHtml()
            // Add speaker info if available
            val personsCount = viewModel.eventDetails.value?.persons?.size ?: 0
            if (personsCount > 0) {
                val personsSummary = event.personsSummary ?: "?"
                val speakersLabel = resources.getQuantityString(R.plurals.speakers, personsCount)
                description = "$speakersLabel: $personsSummary\n\n$description"
            }
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            event.startTime?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it.toEpochMilli()) }
            event.endTime?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it.toEpochMilli()) }
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(requireView(), R.string.calendar_not_found, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showEventDetails(holder: ViewHolder, eventDetails: EventDetails) {
        holder.run {
            val (persons, links) = eventDetails

            // 1. Persons
            if (persons.isNotEmpty()) {
                // Build a list of clickable persons
                val clickablePersonsSummary = buildSpannedString {
                    for (person in persons) {
                        val name = person.name
                        if (name.isNullOrEmpty()) {
                            continue
                        }
                        if (length != 0) {
                            append(", ")
                        }
                        inSpans(PersonClickableSpan(person)) {
                            append(name)
                        }
                    }
                }
                personsTextView.text = clickablePersonsSummary
                personsTextView.isVisible = true
            }

            // 2. Links
            linksContainer.removeAllViews()
            if (links.isNotEmpty()) {
                linksHeader.isVisible = true
                linksContainer.isVisible = true
                val inflater = layoutInflater
                for (link in links) {
                    val view = inflater.inflate(R.layout.item_link, linksContainer, false)
                    view.findViewById<TextView>(R.id.description).apply {
                        text = link.description
                    }
                    view.setOnClickListener(LinkClickListener(event, link))
                    linksContainer += view
                }
            } else {
                linksHeader.isVisible = false
                linksContainer.isVisible = false
            }
        }
    }

    private class PersonClickableSpan(private val person: Person) : ClickableSpan() {
        override fun onClick(v: View) {
            val context = v.context
            val intent = Intent(context, PersonInfoActivity::class.java)
                    .putExtra(PersonInfoActivity.EXTRA_PERSON, person)
            context.startActivity(intent)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }

    private class LinkClickListener(private val event: Event, private val link: Link) : View.OnClickListener {
        override fun onClick(v: View) {
            try {
                val context = v.context
                CustomTabsIntent.Builder()
                        .configureToolbarColors(context, event.track.type.appBarColorResId)
                        .setShowTitle(true)
                        .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                        .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
                        .build()
                        .launchUrl(context, link.url.toUri())
            } catch (ignore: ActivityNotFoundException) {
            }
        }
    }

    companion object {
        private const val ARG_EVENT = "event"
        private const val CONFERENCE_NAME = "FOSDEM"
        private const val CONFERENCE_HASHTAG = "#FOSDEM"
        private const val VENUE_NAME = "ULB"

        fun createArguments(event: Event) = Bundle(1).apply {
            putParcelable(ARG_EVENT, event)
        }
    }
}