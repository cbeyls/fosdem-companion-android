package be.digitalia.fosdem.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class StatusEvent(
        @Embedded
        val event: Event,
        @ColumnInfo(name = "is_bookmarked")
        val isBookmarked: Boolean
)