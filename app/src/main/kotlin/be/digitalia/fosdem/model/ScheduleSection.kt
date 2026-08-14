package be.digitalia.fosdem.model

/**
 * The different parts of schedule data returned by the streaming parser.
 */
sealed interface ScheduleSection {
    class Conference(
        val conferenceId: String,
        val conferenceTitle: String,
        val baseUrl: String
    ) : ScheduleSection

    class Persons(
        val persons: Sequence<PersonDetails>
    ): ScheduleSection

    class Day(
        val day: be.digitalia.fosdem.model.Day,
        val events: Sequence<DetailedEvent>
    ): ScheduleSection
}