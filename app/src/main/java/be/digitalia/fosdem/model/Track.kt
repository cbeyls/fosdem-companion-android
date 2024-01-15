package be.digitalia.fosdem.model

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import be.digitalia.fosdem.R
import kotlinx.parcelize.Parcelize

@Entity(tableName = Track.TABLE_NAME, indices = [Index(value = ["name", "type"], name = "track_main_idx", unique = true)])
@Parcelize
data class Track(
        @PrimaryKey
        val id: Long = 0L,
        val name: String,
        val type: Type
) : Parcelable {

    enum class Type(@StringRes @get:StringRes val nameResId: Int,
                    @ColorRes @get:ColorRes val appBarColorResId: Int,
                    @ColorRes @get:ColorRes val statusBarColorResId: Int,
                    @ColorRes @get:ColorRes val textColorResId: Int) {

        other(R.string.track_type_other,
                R.color.track_type_other, R.color.track_type_other_dark, R.color.track_type_other_text),
        keynote(R.string.track_type_keynote,
                R.color.track_type_keynote, R.color.track_type_keynote_dark, R.color.track_type_keynote_text),
        maintrack(R.string.track_type_main_track,
                R.color.track_type_main, R.color.track_type_main_dark, R.color.track_type_main_text),
        devroom(R.string.track_type_developer_room,
                R.color.track_type_developer_room, R.color.track_type_developer_room_dark, R.color.track_type_developer_room_text),
        lightningtalk(R.string.track_type_lightning_talk,
                R.color.track_type_lightning_talk, R.color.track_type_lightning_talk_dark, R.color.track_type_lightning_talk_text),
        certification(R.string.track_type_certification_exam,
                R.color.track_type_certification_exam, R.color.track_type_certification_exam_dark, R.color.track_type_certification_exam_text),
        junior(R.string.track_type_junior,
            R.color.track_type_junior, R.color.track_type_junior_dark, R.color.track_type_junior_text)

    }

    override fun toString() = name

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + name.hashCode()
        result = prime * result + type.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Track) return false
        return name == other.name && type == other.type
    }

    companion object {
        const val TABLE_NAME = "tracks"
    }
}