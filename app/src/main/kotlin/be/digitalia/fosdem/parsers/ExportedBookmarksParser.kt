package be.digitalia.fosdem.parsers

import be.digitalia.fosdem.ical.ICalendarReader
import okio.BufferedSource

/**
 * Extract event ids from an exported bookmarks file and validate that events match the given application id and year.
 */
class ExportedBookmarksParser(
    private val applicationId: String,
    private val conferenceId: String
) : Parser<LongArray> {

    override fun parse(source: BufferedSource): LongArray {
        val reader = ICalendarReader(source)
        val eventIdList = mutableListOf<String>()

        while (reader.hasNext()) {
            if (reader.selectKey(KEYS) == -1) {
                reader.skipKey()
                reader.skipValue()
            } else {
                val uid = reader.nextValue()
                val parts = uid.split("@")
                // validate UID
                if (parts.size < 3) {
                    throw DataException("Invalid UID format: $uid")
                }
                if (parts[2] != applicationId) {
                    throw DataException("Invalid application id. Expected: $applicationId, actual: ${parts[2]}")
                }
                if (parts[1] != conferenceId) {
                    throw DataException("Invalid conference id. Expected: $conferenceId, actual: ${parts[1]}")
                }
                eventIdList += parts[0]
            }
        }

        return LongArray(eventIdList.size) { eventIdList[it].toLong() }
    }

    class DataException(message: String) : RuntimeException(message)

    companion object {
        private val KEYS = ICalendarReader.Options.of("UID")
    }
}