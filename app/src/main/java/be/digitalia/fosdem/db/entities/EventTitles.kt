package be.digitalia.fosdem.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts3
import androidx.room.PrimaryKey

@Fts3
@Entity(tableName = EventTitles.TABLE_NAME)
class EventTitles(
        @PrimaryKey
        @ColumnInfo(name = "rowid")
        val id: Long,
        val title: String?,
        @ColumnInfo(name = "subtitle")
        val subTitle: String?
) {
    companion object {
        const val TABLE_NAME = "events_titles"
    }
}