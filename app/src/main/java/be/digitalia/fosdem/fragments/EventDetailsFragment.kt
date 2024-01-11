package be.digitalia.fosdem.fragments

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.text.set
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.view.plusAssign
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.PersonInfoActivity
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.model.Building
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.Resource
import be.digitalia.fosdem.model.RoomStatus
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.ClickableArrowKeyMovementMethod
import be.digitalia.fosdem.utils.DateUtils
import be.digitalia.fosdem.utils.configureToolbarColors
import be.digitalia.fosdem.utils.getParcelableCompat
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.utils.parseHtml
import be.digitalia.fosdem.utils.roomNameToResourceName
import be.digitalia.fosdem.utils.stripHtml
import be.digitalia.fosdem.utils.toLocalDateTimeOrNull
import be.digitalia.fosdem.viewmodels.EventDetailsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class EventDetailsFragment : Fragment(R.layout.fragment_event_details) {

    private class ViewHolder(view: View) {
        val personsTextView: TextView = view.findViewById(R.id.persons)
        val timeTextView: TextView = view.findViewById(R.id.time)
        val roomStatusTextView: TextView = view.findViewById(R.id.room_status)
        val attachmentsHeader: View = view.findViewById(R.id.attachments_header)
        val attachmentsContainer: ViewGroup = view.findViewById(R.id.attachments_container)
        val linksHeader: View = view.findViewById(R.id.links_header)
        val linksContainer: ViewGroup = view.findViewById(R.id.links_container)
        val resourcesFooter: View = view.findViewById(R.id.resources_footer)
    }

    @Inject
    lateinit var userSettingsProvider: UserSettingsProvider
    @Inject
    lateinit var api: FosdemApi
    private val viewModel: EventDetailsViewModel by viewModels(extrasProducer = {
        defaultViewModelCreationExtras.withCreationCallback<EventDetailsViewModel.Factory> { factory ->
            factory.create(event)
        }
    })

    val event by lazy<Event>(LazyThreadSafetyMode.NONE) {
        requireArguments().getParcelableCompat(ARG_EVENT)!!
    }

    private inner class EventDetailsMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.event, menu)
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

        override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
            R.id.add_to_agenda -> {
                addToAgenda()
                true
            }
            else -> false
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(EventDetailsMenuProvider(), viewLifecycleOwner)

        val timeFormatter = DateUtils.getTimeFormatter(view.context)

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
                    movementMethod = LinkMovementMethodCompat.getInstance()
                    isVisible = true
                }
            }

            // Display time placeholder until the ZoneId is loaded from user preferences
            bindTime(timeTextView, timeFormatter, null)

            view.findViewById<TextView>(R.id.room).apply {
                val roomName = event.roomName
                if (roomName.isNullOrEmpty()) {
                    isVisible = false
                } else {
                    val building = Building.fromRoomName(roomName)
                    val roomText = SpannableString(if (building == null) roomName else getString(R.string.room_building, roomName, building))
                    val roomImageResId = resources.getIdentifier(roomNameToResourceName(roomName), "drawable", requireActivity().packageName)

                    // Make the room text clickable to display the room image, even if it doesn't exist
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
                    movementMethod = LinkMovementMethodCompat.getInstance()

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

        viewLifecycleOwner.lifecycleScope.launch {
            val eventDetails = viewModel.eventDetails.await()
            viewLifecycleOwner.withStarted {
                showEventDetails(holder, eventDetails)
            }
        }

        val roomName = event.roomName
        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            launch {
                userSettingsProvider.zoneId.collect { zoneId ->
                    bindTime(holder.timeTextView, timeFormatter, zoneId)
                }
            }

            // Live room status
            if (!roomName.isNullOrEmpty()) {
                launch {
                    api.roomStatuses.collect { statuses ->
                        bindRoomStatus(holder.roomStatusTextView, statuses[roomName])
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindTime(timeTextView: TextView, timeFormatter: DateTimeFormatter, zoneId: ZoneId?) {
        val startTime = event.startTime?.toLocalDateTimeOrNull(zoneId)?.format(timeFormatter) ?: "?"
        val endTime = event.endTime?.toLocalDateTimeOrNull(zoneId)?.format(timeFormatter) ?: "?"
        timeTextView.text = "${event.day}, $startTime ― $endTime"
        timeTextView.contentDescription = getString(R.string.time_content_description, timeTextView.text)
    }

    private fun bindRoomStatus(roomStatusTextView: TextView, roomStatus: RoomStatus?) {
        if (roomStatus == null) {
            roomStatusTextView.text = null
        } else {
            roomStatusTextView.setText(roomStatus.nameResId)
            roomStatusTextView.setTextColor(
                ContextCompat.getColorStateList(roomStatusTextView.context, roomStatus.colorResId)
            )
        }
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
            val personsSummary = event.personsSummary
            if (!personsSummary.isNullOrBlank()) {
                val personsCount = personsSummary.count { it == ',' } + 1
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
            val (persons, attachments, links) = eventDetails

            // 1. Persons
            if (persons.isNotEmpty()) {
                // Build a list of clickable persons
                val clickablePersonsSummary = buildSpannedString {
                    for (person in persons) {
                        val name = person.name
                        if (name.isNullOrEmpty()) {
                            continue
                        }
                        if (isNotEmpty()) {
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

            // 2. Attachments
            populateResources(attachments, R.layout.item_attachment, attachmentsHeader, attachmentsContainer)

            // 3. Links
            populateResources(links, R.layout.item_link, linksHeader, linksContainer)

            resourcesFooter.isVisible = attachments.isNotEmpty() || links.isNotEmpty()
        }
    }

    private fun populateResources(
        resources: List<Resource>,
        @LayoutRes layoutResId: Int,
        header: View,
        container: ViewGroup
    ) {
        container.removeAllViews()
        if (resources.isNotEmpty()) {
            header.isVisible = true
            container.isVisible = true
            val inflater = layoutInflater
            for (resource in resources) {
                val view = inflater.inflate(layoutResId, container, false)
                view.findViewById<TextView>(R.id.description).apply {
                    text = resource.description
                }
                view.setOnClickListener(ResourceClickListener(event, resource))
                container += view
            }
        } else {
            header.isVisible = false
            container.isVisible = false
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

    private class ResourceClickListener(
        private val event: Event,
        private val resource: Resource
    ) : View.OnClickListener {
        override fun onClick(v: View) {
            try {
                val context = v.context
                CustomTabsIntent.Builder()
                        .configureToolbarColors(context, event.track.type.appBarColorResId)
                        .setShowTitle(true)
                        .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                        .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
                        .build()
                        .launchUrl(context, resource.url.toUri())
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