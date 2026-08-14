package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.PrimaryKey
import kotlinx.parcelize.Parcelize

@Fts4
@Entity(tableName = Person.TABLE_NAME)
@Parcelize
data class Person(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val id: Long,
    val name: String?
) : Parcelable {

    override fun toString(): String = name.orEmpty()

    companion object {
        const val TABLE_NAME = "persons"
    }
}