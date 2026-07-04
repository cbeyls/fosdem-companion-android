package be.digitalia.fosdem.settings

import androidx.core.graphics.ColorUtils
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.db.entities.RoomColor
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistent room-to-color mappings.
 * Colors are assigned to maximize visual distinction from existing colors.
 */
@Singleton
class RoomColorManager @Inject constructor(
    private val scheduleDao: ScheduleDao
) {
    private companion object {
        const val SATURATION = 0.65f
        const val LIGHTNESS = 0.45f
    }

    // Cache of room name -> hue (0-360)
    private val roomHues = mutableMapOf<String, Float>()

    init {
        // Load existing mappings from database
        runBlocking {
            loadRoomHues()
        }
    }

    private suspend fun loadRoomHues() {
        for (roomColor in scheduleDao.getAllRoomColors()) {
            roomHues[roomColor.roomName] = roomColor.hue
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
            runBlocking {
                scheduleDao.insertRoomColor(RoomColor(roomName, newHue))
            }
            newHue
        }
        return ColorUtils.HSLToColor(floatArrayOf(hue, SATURATION, LIGHTNESS))
    }

    /**
     * Reloads the in-memory cache from the database.
     * Should be called after a schedule refresh that may have purged stale room colors.
     */
    suspend fun reloadCache() {
        roomHues.clear()
        loadRoomHues()
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
