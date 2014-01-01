package be.digitalia.fosdem.api;

import java.util.Locale;

/**
 * This class contains all FOSDEM Urls
 * 
 * @author Christophe Beyls
 * 
 */
public class FosdemUrls {

	private static final String SCHEDULE_URL = "https://fosdem.org/schedule/xml";
	private static final String EVENT_URL_FORMAT = "https://fosdem.org/%1$d/schedule/event/%2$s/";
	private static final String PERSON_URL_FORMAT = "https://fosdem.org/%1$d/schedule/speaker/%2$s/";

	public static String getSchedule() {
		return SCHEDULE_URL;
	}

	public static String getEvent(String slug, int year) {
		return String.format(Locale.US, EVENT_URL_FORMAT, year, slug);
	}

	public static String getPerson(String slug, int year) {
		return String.format(Locale.US, PERSON_URL_FORMAT, year, slug);
	}
}
