package be.digitalia.fosdem.parsers

import android.util.JsonReader
import be.digitalia.fosdem.model.RoomStatus
import okio.BufferedSource
import java.io.InputStreamReader

class RoomStatusesParser : Parser<Map<String, RoomStatus>> {

    override fun parse(source: BufferedSource): Map<String, RoomStatus> {
        val reader = JsonReader(InputStreamReader(source.inputStream()))
        val result = mutableMapOf<String, RoomStatus>()

        reader.beginArray()
        while (reader.hasNext()) {
            var roomName: String? = null
            var roomStatus: RoomStatus? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "roomname" -> roomName = reader.nextString()
                    "state" -> {
                        val stateValue = reader.nextString()
                        try {
                            roomStatus = RoomStatus.values()[stateValue.toInt()]
                        } catch (e: Exception) {
                            // Swallow and ignore that room
                        }
                    }
                    else -> reader.skipValue()
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
}