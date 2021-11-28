package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.utils.toSlug
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

    fun getUrl(year: Int): String? {
        return name?.let { FosdemUrls.getPerson(it.toSlug(), year) }
    }

    override fun toString(): String = name.orEmpty()

    companion object {
        const val TABLE_NAME = "persons"
    }
}