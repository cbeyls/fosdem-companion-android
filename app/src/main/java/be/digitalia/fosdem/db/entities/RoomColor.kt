package be.digitalia.fosdem.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = RoomColor.TABLE_NAME)
class RoomColor(
    @PrimaryKey
    @ColumnInfo(name = "room_name")
    val roomName: String,
    val hue: Float
) {
    companion object {
        const val TABLE_NAME = "room_colors"
    }
}
