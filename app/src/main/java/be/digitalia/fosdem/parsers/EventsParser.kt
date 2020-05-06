package be.digitalia.fosdem.parsers

import be.digitalia.fosdem.model.*
import be.digitalia.fosdem.utils.*
import be.digitalia.fosdem.utils.DateUtils.belgiumTimeZone
import be.digitalia.fosdem.utils.DateUtils.withBelgiumTimeZone
import okio.BufferedSource
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main parser for FOSDEM schedule data in pentabarf XML format.
 *
 * @author Christophe Beyls
 */
class EventsParser : Parser<Sequence<DetailedEvent>> {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).withBelgiumTimeZone()
    // Calendar used to compute the events time, according to Belgium timezone
    private val calendar = Calendar.getInstance(belgiumTimeZone, Locale.US)

    override fun parse(source: BufferedSource): Sequence<DetailedEvent> {
        val parser: XmlPullParser = xmlPullParserFactory.newPullParser().apply {
            setInput(source.inputStream(), null)
        }
        return sequence {
            while (!parser.isEndDocument) {
                if (parser.isStartTag("schedule")) {
                    var currentDay: Day? = null
                    var currentRoomName: String? = null

                    while (!parser.isNextEndTag("schedule")) {
                        if (parser.isStartTag) {
                            when (parser.name) {
                                "day" -> {
                                    currentDay = Day(
                                            index = parser.getAttributeValue(null, "index")!!.toInt(),
                                            date = dateFormat.parse(parser.getAttributeValue(null, "date"))!!
                                    )
                                }
                                "room" -> currentRoomName = parser.getAttributeValue(null, "name")
                                "event" -> yield(parseEvent(parser, currentDay!!, currentRoomName))
                                else -> parser.skipToEndTag()
                            }
                        }
                    }
                }
                parser.next()
            }
        }
    }

    private fun parseEvent(parser: XmlPullParser, day: Day, roomName: String?): DetailedEvent {
        val id = parser.getAttributeValue(null, "id")!!.toLong()
        var startTime: Date? = null
        var duration: String? = null
        var slug: String? = null
        var title: String? = null
        var subTitle: String? = null
        var trackName = ""
        var trackType = Track.Type.other
        var abstractText: String? = null
        var description: String? = null
        val persons = mutableListOf<Person>()
        val links = mutableListOf<Link>()

        while (!parser.isNextEndTag("event")) {
            if (parser.isStartTag) {
                when (parser.name) {
                    "start" -> {
                        val timeString = parser.nextText()
                        if (!timeString.isNullOrEmpty()) {
                            startTime = with(calendar) {
                                time = day.date
                                set(Calendar.HOUR_OF_DAY, getHours(timeString))
                                set(Calendar.MINUTE, getMinutes(timeString))
                                time
                            }
                        }
                    }
                    "duration" -> duration = parser.nextText()
                    "slug" -> slug = parser.nextText()
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
                    "persons" -> while (!parser.isNextEndTag("persons")) {
                        if (parser.isStartTag("person")) {
                            val person = Person(
                                    id = parser.getAttributeValue(null, "id")!!.toLong(),
                                    name = parser.nextText()!!
                            )
                            persons += person
                        }
                    }
                    "links" -> while (!parser.isNextEndTag("links")) {
                        if (parser.isStartTag("link")) {
                            val link = Link(
                                    eventId = id,
                                    url = parser.getAttributeValue(null, "href")!!,
                                    description = parser.nextText()
                            )
                            links += link
                        }
                    }
                    else -> parser.skipToEndTag()
                }
            }
        }

        val endTime = if (startTime != null && !duration.isNullOrEmpty()) {
            with(calendar) {
                add(Calendar.HOUR_OF_DAY, getHours(duration))
                add(Calendar.MINUTE, getMinutes(duration))
                time
            }
        } else null

        val event = Event(
                id = id,
                day = day,
                roomName = roomName,
                startTime = startTime,
                endTime = endTime,
                slug = slug,
                title = title,
                subTitle = subTitle,
                track = Track(trackName, trackType),
                abstractText = abstractText,
                description = description,
                personsSummary = null
        )
        val details = EventDetails(
                persons = persons,
                links = links
        )
        return DetailedEvent(event, details)
    }

    /**
     * Returns the hours portion of a time string in the "hh:mm" format, without allocating objects.
     *
     * @param time string in the "hh:mm" format
     * @return hours
     */
    private fun getHours(time: String): Int {
        return Character.getNumericValue(time[0]) * 10 + Character.getNumericValue(time[1])
    }

    /**
     * Returns the minutes portion of a time string in the "hh:mm" format, without allocating objects.
     *
     * @param time string in the "hh:mm" format
     * @return minutes
     */
    private fun getMinutes(time: String): Int {
        return Character.getNumericValue(time[3]) * 10 + Character.getNumericValue(time[4])
    }
}