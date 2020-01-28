package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts3
import androidx.room.PrimaryKey
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.utils.toSlug
import kotlinx.android.parcel.Parcelize

@Fts3
@Entity(tableName = Person.TABLE_NAME)
@Parcelize
data class Person(
        @PrimaryKey
        @ColumnInfo(name = "rowid")
        val id: Long,
        val name: String?
) : Parcelable {

    fun getUrl(year: Int): String? {
        val n = name ?: return null
        return FosdemUrls.getPerson(n.toSlug(), year)
    }

    override fun toString(): String = name ?: ""

    companion object {
        const val TABLE_NAME = "persons"
    }
}