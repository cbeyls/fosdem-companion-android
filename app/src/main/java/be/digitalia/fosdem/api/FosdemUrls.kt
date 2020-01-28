package be.digitalia.fosdem.api

/**
 * This class contains all FOSDEM Urls
 *
 * @author Christophe Beyls
 */
object FosdemUrls {

    val schedule
        get() = "https://fosdem.org/schedule/xml"
    val rooms
        get() = "https://api.fosdem.org/roomstatus/v1/listrooms"
    val localNavigation
        get() = "https://nav.fosdem.org/"
    val volunteer
        get() = "https://fosdem.org/volunteer/"

    fun getEvent(slug: String, year: Int): String {
        return "https://fosdem.org/$year/schedule/event/$slug/"
    }

    fun getPerson(slug: String, year: Int): String {
        return "https://fosdem.org/$year/schedule/speaker/$slug/"
    }

    fun getLocalNavigationToLocation(locationSlug: String): String {
        return "https://nav.fosdem.org/d/$locationSlug/"
    }
}