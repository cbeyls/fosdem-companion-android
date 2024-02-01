package be.digitalia.fosdem.parsers

import be.digitalia.fosdem.model.Attachment
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.DetailedEvent
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.EventDetails
import be.digitalia.fosdem.model.Link
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.Schedule
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.isEndDocument
import be.digitalia.fosdem.utils.isNextEndTag
import be.digitalia.fosdem.utils.isStartTag
import be.digitalia.fosdem.utils.skipToEndTag
import be.digitalia.fosdem.utils.xmlPullParserFactory
import okio.BufferedSource
import org.xmlpull.v1.XmlPullParser
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Main parser for FOSDEM schedule data in pretalx XML format.
 *
 * @author Christophe Beyls
 */
class ScheduleParser @Inject constructor() : Parser<Schedule> {

    override fun parse(source: BufferedSource): Schedule {
        val parser: XmlPullParser = xmlPullParserFactory.newPullParser().apply {
            setInput(source.inputStream(), null)
        }

        while (!parser.isEndDocument) {
            if (parser.isStartTag("schedule")) {
                while (!parser.isNextEndTag("schedule")) {
                    if (parser.isStartTag) {
                        if (parser.name == "conference") {
                            var conferenceId: String? = null
                            var conferenceTitle: String? = null
                            var baseUrl: String? = null

                            while (!parser.isNextEndTag("conference")) {
                                if (parser.isStartTag) {
                                    when (parser.name) {
                                        "acronym" -> conferenceId = parser.nextText()
                                        "title" -> conferenceTitle = parser.nextText()
                                        "base_url" -> baseUrl = parser.nextText()
                                        else -> parser.skipToEndTag()
                                    }
                                }
                            }

                            return Schedule(
                                conferenceId = checkNotNull(conferenceId) { "Missing conference acronym" },
                                conferenceTitle = checkNotNull(conferenceTitle) { "Missing conference title" },
                                baseUrl = checkNotNull(baseUrl) { "Missing conference base_url" },
                                events = parseEvents(parser)
                            )
                        } else {
                            parser.skipToEndTag()
                        }
                    }
                }
            }
            parser.next()
        }

        throw IllegalStateException("Missing conference node")
    }

    private fun parseEvents(parser: XmlPullParser): Sequence<DetailedEvent> = sequence {
        while (!parser.isEndDocument) {
            while (!parser.isNextEndTag("schedule")) {
                if (parser.isStartTag) {
                    if (parser.name == "day") {
                        val day = Day(
                            index = parser.getAttributeValue(null, "index")!!.toInt(),
                            date = LocalDate.parse(
                                parser.getAttributeValue(null, "date")
                            ),
                            startTime = OffsetDateTime.parse(
                                parser.getAttributeValue(null, "start")
                            ).toInstant(),
                            endTime = OffsetDateTime.parse(
                                parser.getAttributeValue(null, "end")
                            ).toInstant()
                        )

                        while (!parser.isNextEndTag("day")) {
                            if (parser.isStartTag("room")) {
                                val roomName: String? =
                                    parser.getAttributeValue(null, "name")

                                while (!parser.isNextEndTag("room")) {
                                    if (parser.isStartTag("event")) {
                                        yield(parseEvent(parser, day, roomName))
                                    }
                                }
                            }
                        }
                    } else {
                        parser.skipToEndTag()
                    }
                }
            }
            parser.next()
        }
    }

    private fun parseEvent(parser: XmlPullParser, day: Day, roomName: String?): DetailedEvent {
        val id = parser.getAttributeValue(null, "id")!!.toLong()
        var startTime: Instant? = null
        var startTimeOffset: ZoneOffset? = null
        var duration: String? = null
        var url: String? = null
        var title: String? = null
        var subTitle: String? = null
        var trackName = ""
        var trackType = Track.Type.other
        var abstractText: String? = null
        var description: String? = null
        var feedbackUrl: String? = null
        val persons = mutableListOf<Person>()
        val attachments = mutableListOf<Attachment>()
        val links = mutableListOf<Link>()

        while (!parser.isNextEndTag("event")) {
            if (parser.isStartTag) {
                when (parser.name) {
                    "date" -> {
                        val dateTimeString = parser.nextText()
                        if (!dateTimeString.isNullOrEmpty()) {
                            val dateTime = OffsetDateTime.parse(dateTimeString)
                            startTime = dateTime.toInstant()
                            startTimeOffset = dateTime.offset
                        }
                    }
                    "duration" -> duration = parser.nextText()
                    "url" -> url = parser.nextText()
                    "title" -> title = parser.nextText()
                    "subtitle" -> subTitle = parser.nextText()
                    "track" -> trackName = parser.nextText()
                    "type" -> try {
                        trackType = enumValueOf(parser.nextText())
                    } catch (e: Exception) {
                        // trackType will be "other"
                    }
                    "abstract" -> abstractText = parser.nextText()
                    "description" -> description = parser.nextText()
                    "feedback_url" -> feedbackUrl = parser.nextText()
                    "persons" -> while (!parser.isNextEndTag("persons")) {
                        if (parser.isStartTag("person")) {
                            val person = Person(
                                    id = parser.getAttributeValue(null, "id")!!.toLong(),
                                    name = parser.nextText()!!
                            )
                            persons += person
                        }
                    }
                    "attachments" -> while (!parser.isNextEndTag("attachments")) {
                        if (parser.isStartTag("attachment")) {
                            val attachmentType = parser.getAttributeValue(null, "type")
                            attachments += Attachment(
                                eventId = id,
                                url = parser.getAttributeValue(null, "href")!!,
                                description = parser.nextText().let { attachmentDescription ->
                                    // Use the type to replace the description if absent
                                    if (attachmentDescription.isNullOrBlank() && attachmentType != null) {
                                        attachmentType.replaceFirstChar { it.uppercaseChar() }
                                    } else {
                                        attachmentDescription
                                    }
                                }
                            )
                        }
                    }
                    "links" -> while (!parser.isNextEndTag("links")) {
                        if (parser.isStartTag("link")) {
                            val linkUrl = parser.getAttributeValue(null, "href")!!
                            val linkDescription = parser.nextText()
                            // Feedback URL is already handled using its dedicated field
                            if (url != feedbackUrl) {
                                links += Link(
                                    eventId = id,
                                    url = linkUrl,
                                    description = linkDescription
                                )
                            }
                        }
                    }
                    else -> parser.skipToEndTag()
                }
            }
        }

        val endTime = if (startTime != null && !duration.isNullOrEmpty()) {
            startTime + Duration.ofSeconds(parseTimeAsSeconds(duration))
        } else null

        val event = Event(
            id = id,
            day = day,
            roomName = roomName,
            startTime = startTime,
            startTimeOffset = startTimeOffset,
            endTime = endTime,
            url = url,
            title = title,
            subTitle = subTitle,
            track = Track(name = trackName, type = trackType),
            abstractText = abstractText,
            description = description,
            feedbackUrl = feedbackUrl,
            personsSummary = null
        )
        val details = EventDetails(
            persons = persons,
            attachments = attachments,
            links = links
        )
        return DetailedEvent(event, details)
    }

    /**
     * Return the total number of seconds of a string in the "hh:mm" or "hh:mm:ss" format,
     * without allocating heap memory.
     *
     * @param time string in the "hh:mm" or "hh:mm:ss" format
     */
    private fun parseTimeAsSeconds(time: String): Long {
        // hours
        var result = time[0].digitToInt() * 10 + time[1].digitToInt()
        // minutes
        result = result * 60 + time[3].digitToInt() * 10 + time[4].digitToInt()
        // seconds
        result *= 60
        if (time.length >= 8) {
            result += time[6].digitToInt() * 10 + time[7].digitToInt()
        }
        return result.toLong()
    }
}