package be.digitalia.fosdem.api

import java.net.URLEncoder

/**
 * This class contains all FOSDEM Urls
 *
 * @author Christophe Beyls
 */
object FosdemUrls {

    const val schedule = "https://fosdem.org/schedule/xml"
    const val rooms = "https://api.fosdem.org/roomstatus/v1/listrooms"
    const val localNavigation = "https://nav.fosdem.org/"
    const val stands = "https://fosdem.org/stands/"
    const val volunteer = "https://fosdem.org/volunteer/"

    fun getPerson(baseUrl: String, slug: String): String {
        return "${baseUrl}speaker/$slug/"
    }

    fun getPersonUri(baseUrl: String, name: String): String {
        return "${baseUrl}speaker/#" + URLEncoder.encode(name, Charsets.UTF_8.toString())
    }

    fun getLocalNavigationToLocation(locationSlug: String): String {
        return "https://nav.fosdem.org/d/$locationSlug/"
    }

    fun getStands(year: Int): String {
        return "https://fosdem.org/$year/stands/"
    }
}