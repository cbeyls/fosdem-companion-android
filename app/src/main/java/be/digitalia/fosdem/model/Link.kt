package be.digitalia.fosdem.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "links",
    indices = [
        Index(value = ["event_id"], name = "link_event_id_idx")
    ]
)
@Parcelize
data class Link(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0L,
        @ColumnInfo(name = "event_id")
        val eventId: Long,
        override val url: String,
        override val description: String?
) : Resource {

    companion object {
        const val TABLE_NAME = "links"
    }
}