package be.digitalia.fosdem.parsers

import be.digitalia.fosdem.model.RoomStatus
import com.squareup.moshi.JsonReader
import okio.BufferedSource

class RoomStatusesParser : Parser<Map<String, RoomStatus>> {

    override fun parse(source: BufferedSource): Map<String, RoomStatus> {
        val reader = JsonReader.of(source)
        val result = mutableMapOf<String, RoomStatus>()

        reader.beginArray()
        while (reader.hasNext()) {
            var roomName: String? = null
            var roomStatus: RoomStatus? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.selectName(PROPERTIES_NAMES)) {
                    PROPERTIES_NAME_ROOM_NAME -> roomName = reader.nextString()
                    PROPERTIES_NAME_STATE -> {
                        val stateValue = reader.nextInt()
                        try {
                            roomStatus = RoomStatus.entries[stateValue]
                        } catch (e: Exception) {
                            // Swallow and ignore that room
                        }
                    }
                    else -> {
                        reader.skipName()
                        reader.skipValue()
                    }
                }
            }
            reader.endObject()

            if (roomName != null && roomStatus != null) {
                result[roomName] = roomStatus
            }
        }
        reader.endArray()

        return result
    }

    companion object {
        private val PROPERTIES_NAMES = JsonReader.Options.of(
                "roomname", "state"
        )
        private const val PROPERTIES_NAME_ROOM_NAME = 0
        private const val PROPERTIES_NAME_STATE = 1
    }
}