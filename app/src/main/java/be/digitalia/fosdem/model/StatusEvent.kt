package be.digitalia.fosdem.model

import androidx.room3.ColumnInfo
import androidx.room3.Embedded

data class StatusEvent(
        @Embedded
        val event: Event,
        @ColumnInfo(name = "is_bookmarked")
        val isBookmarked: Boolean
)