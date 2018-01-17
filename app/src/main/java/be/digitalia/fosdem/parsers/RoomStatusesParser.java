package be.digitalia.fosdem.parsers;

import android.util.JsonReader;

import java.util.HashMap;
import java.util.Map;

import be.digitalia.fosdem.model.RoomStatus;

public class RoomStatusesParser extends AbstractJsonPullParser<Map<String, RoomStatus>> {

	@Override
	protected Map<String, RoomStatus> parse(JsonReader reader) throws Exception {
		Map<String, RoomStatus> result = new HashMap<>();

		reader.beginArray();
		while (reader.hasNext()) {
			String roomName = null;
			RoomStatus roomStatus = null;

			reader.beginObject();
			while (reader.hasNext()) {
				switch (reader.nextName()) {
					case "roomname":
						roomName = reader.nextString();
						break;
					case "state":
						String stateValue = reader.nextString();
						try {
							roomStatus = RoomStatus.values()[Integer.parseInt(stateValue)];
						} catch (Exception e) {
							// Swallow and ignore that room
						}
						break;
					default:
						reader.skipValue();
				}
			}
			reader.endObject();

			if (roomName != null && roomStatus != null) {
				result.put(roomName, roomStatus);
			}
		}
		reader.endArray();

		return result;
	}
}
