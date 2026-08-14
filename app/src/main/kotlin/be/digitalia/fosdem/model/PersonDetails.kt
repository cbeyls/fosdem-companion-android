package be.digitalia.fosdem.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = PersonDetails.TABLE_NAME)
data class PersonDetails(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val id: Long,
    val slug: String?,
    val biography: String?,
) {
    companion object {
        const val TABLE_NAME = "person_details"
    }
}