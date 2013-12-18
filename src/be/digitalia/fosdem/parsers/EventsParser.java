package be.digitalia.fosdem.parsers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;

import android.text.TextUtils;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.Track;

/**
 * Main parser for FOSDEM schedule data in pentabarf XML format.
 * 
 * @author Christophe Beyls
 * 
 */
public class EventsParser extends IterableAbstractPullParser<Event> {

	private static final String BELGIUM_TIMEZONE = "GMT+1";
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	{
		// All dates and times are in Belgium timezone
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(BELGIUM_TIMEZONE));
	}

	// Calendar used to compute the events time, according to Belgium timezone
	private final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(BELGIUM_TIMEZONE), Locale.US);

	private Day currentDay;
	private String currentRoom;
	private Track currentTrack;
	// Recycled objects (improving parser performance)
	private List<Person> persons = new ArrayList<Person>();
	private List<Link> links = new ArrayList<Link>();

	/**
	 * Returns the hours portion of a time string in the "hh:mm" format, without allocating objects.
	 * 
	 * @param time
	 * @return hours
	 */
	private static int getHours(String time) {
		return (Character.getNumericValue(time.charAt(0)) * 10) + Character.getNumericValue(time.charAt(1));
	}

	/**
	 * Returns the minutes portion of a time string in the "hh:mm" format, without allocating objects.
	 * 
	 * @param time
	 * @return minutes
	 */
	private static int getMinutes(String time) {
		return (Character.getNumericValue(time.charAt(3)) * 10) + Character.getNumericValue(time.charAt(4));
	}

	@Override
	protected boolean parseHeader(XmlPullParser parser) throws Exception {
		while (!isEndDocument()) {
			if (isStartTag("schedule")) {
				return true;
			}

			parser.next();
		}
		return false;
	}

	@Override
	protected Event parseNext(XmlPullParser parser) throws Exception {
		while (!isNextEndTag("schedule")) {
			if (isStartTag()) {
				String name = parser.getName();

				if ("day".equals(name)) {
					currentDay = new Day();
					currentDay.setIndex(Integer.parseInt(parser.getAttributeValue(null, "index")));
					currentDay.setDate(DATE_FORMAT.parse(parser.getAttributeValue(null, "date")));
				} else if ("room".equals(name)) {
					currentRoom = parser.getAttributeValue(null, "name");
				} else if ("event".equals(name)) {
					Event event = new Event();
					event.setId(Integer.parseInt(parser.getAttributeValue(null, "id")));
					event.setDay(currentDay);
					event.setRoomName(currentRoom);
					event.setPersons(persons);
					event.setLinks(links);
					// reset persons and links list
					persons.clear();
					links.clear();
					String duration = null;
					String trackName = "";
					Track.Type trackType = Track.Type.other;

					while (!isNextEndTag("event")) {
						if (isStartTag()) {
							name = parser.getName();

							if ("start".equals(name)) {
								String time = parser.nextText();
								if (!TextUtils.isEmpty(time)) {
									calendar.setTime(currentDay.getDate());
									calendar.set(Calendar.HOUR_OF_DAY, getHours(time));
									calendar.set(Calendar.MINUTE, getMinutes(time));
									event.setStartTime(calendar.getTime());
								}
							} else if ("duration".equals(name)) {
								duration = parser.nextText();
							} else if ("slug".equals(name)) {
								event.setSlug(parser.nextText());
							} else if ("title".equals(name)) {
								event.setTitle(parser.nextText());
							} else if ("subtitle".equals(name)) {
								event.setSubTitle(parser.nextText());
							} else if ("track".equals(name)) {
								trackName = parser.nextText();
							} else if ("type".equals(name)) {
								try {
									trackType = Enum.valueOf(Track.Type.class, parser.nextText());
								} catch (Exception e) {
									// trackType will be "other"
								}
							} else if ("abstract".equals(name)) {
								event.setAbstractText(parser.nextText());
							} else if ("description".equals(name)) {
								event.setDescription(parser.nextText());
							} else if ("persons".equals(name)) {
								while (!isNextEndTag("persons")) {
									if (isStartTag("person")) {
										Person person = new Person();
										person.setId(Integer.parseInt(parser.getAttributeValue(null, "id")));
										person.setName(parser.nextText());

										persons.add(person);
									}
								}
							} else if ("links".equals(name)) {
								while (!isNextEndTag("links")) {
									if (isStartTag("link")) {
										Link link = new Link();
										link.setUrl(parser.getAttributeValue(null, "href"));
										link.setDescription(parser.nextText());

										links.add(link);
									}
								}
							} else {
								skipToEndTag();
							}
						}
					}

					if ((event.getStartTime() != null) && !TextUtils.isEmpty(duration)) {
						calendar.add(Calendar.HOUR_OF_DAY, getHours(duration));
						calendar.add(Calendar.MINUTE, getMinutes(duration));
						event.setEndTime(calendar.getTime());
					}

					if ((currentTrack == null) || !trackName.equals(currentTrack.getName()) || (trackType != currentTrack.getType())) {
						currentTrack = new Track(trackName, trackType);
					}
					event.setTrack(currentTrack);

					return event;
				} else {
					skipToEndTag();
				}
			}
		}
		return null;
	}
}
