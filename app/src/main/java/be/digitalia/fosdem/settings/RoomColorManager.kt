package be.digitalia.fosdem.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manages persistent room-to-color mappings.
 * Colors are assigned to maximize visual distinction from existing colors.
 */
@Singleton
class RoomColorManager @Inject constructor(
    @param:Named("UIState") private val preferences: SharedPreferences
) {
    private companion object {
        const val KEY_PREFIX = "room_color_"
        const val SATURATION = 0.65f
        const val LIGHTNESS = 0.45f
    }

    // Cache of room name -> hue (0-360)
    private val roomHues = mutableMapOf<String, Float>()

    init {
        // Load existing mappings from preferences
        preferences.all.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX) && value is Float) {
                val roomName = key.removePrefix(KEY_PREFIX)
                roomHues[roomName] = value
            }
        }
    }

    /**
     * Gets a consistent color for a room name.
     * If the room hasn't been seen before, assigns a new color that maximizes
     * distinction from existing colors and persists it.
     */
    fun getColorForRoom(roomName: String): Int {
        val hue = roomHues.getOrPut(roomName) {
            val newHue = findOptimalHue()
            preferences.edit {
                putFloat(KEY_PREFIX + roomName, newHue)
            }
            newHue
        }
        return ColorUtils.HSLToColor(floatArrayOf(hue, SATURATION, LIGHTNESS))
    }

    /**
     * Removes color mappings for rooms not in [roomNames], keeping existing colors stable.
     */
    fun retainOnly(roomNames: Set<String>) {
        val staleRooms = roomHues.keys - roomNames
        if (staleRooms.isEmpty()) return
        for (room in staleRooms) {
            roomHues.remove(room)
        }
        preferences.edit {
            for (room in staleRooms) {
                remove(KEY_PREFIX + room)
            }
        }
    }

    /**
     * Finds the optimal hue that maximizes the minimum distance from all existing hues.
     * This ensures new colors are as visually distinct as possible.
     */
    private fun findOptimalHue(): Float {
        val existingHues = roomHues.values.toList()

        if (existingHues.isEmpty()) {
            return 0f
        }

        if (existingHues.size == 1) {
            // Place opposite on the color wheel
            return (existingHues[0] + 180f) % 360f
        }

        // Sort hues and find the largest gap (treating the color wheel as circular)
        val sortedHues = existingHues.sorted()

        var maxGap = 0f
        var gapStart = 0f

        // Check gaps between consecutive hues
        for (i in 0 until sortedHues.size - 1) {
            val gap = sortedHues[i + 1] - sortedHues[i]
            if (gap > maxGap) {
                maxGap = gap
                gapStart = sortedHues[i]
            }
        }

        // Check the wrap-around gap (from last hue to first hue + 360)
        val wrapGap = (360f - sortedHues.last()) + sortedHues.first()
        if (wrapGap > maxGap) {
            maxGap = wrapGap
            gapStart = sortedHues.last()
        }

        // Place new hue in the middle of the largest gap
        return (gapStart + maxGap / 2f) % 360f
    }
}
