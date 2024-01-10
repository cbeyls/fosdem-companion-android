package be.digitalia.fosdem.model

/**
 * The complete schedule data returned by the streaming parser.
 */
class Schedule(
    val conferenceId: String,
    val conferenceTitle: String,
    val baseUrl: String,
    val events: Sequence<DetailedEvent>
)