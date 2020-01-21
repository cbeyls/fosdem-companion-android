package be.digitalia.fosdem.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Bookmark.TABLE_NAME)
class Bookmark(
        @PrimaryKey
        @ColumnInfo(name = "event_id")
        val eventId: Long
) {
    companion object {
        const val TABLE_NAME = "bookmarks"
    }
}