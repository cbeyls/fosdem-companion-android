package be.digitalia.fosdem.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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