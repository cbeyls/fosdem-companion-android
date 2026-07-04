package be.digitalia.fosdem.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.PrimaryKey

@Fts4
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