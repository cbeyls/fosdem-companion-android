package be.digitalia.fosdem.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
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