package be.digitalia.fosdem.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.digitalia.fosdem.R

enum class RoomStatus(
    @param:StringRes @get:StringRes val nameResId: Int,
    @param:ColorRes @get:ColorRes val colorResId: Int,
    @param:DrawableRes @get:DrawableRes val iconResId: Int
) {
    OPEN(R.string.room_status_open, R.color.room_status_open, 0),
    FULL(R.string.room_status_full, R.color.room_status_full, R.drawable.ic_warning_white_18sp),
    EMERGENCY_EVACUATION(
        R.string.room_status_emergency_evacuation,
        R.color.room_status_emergency_evacuation,
        R.drawable.ic_warning_white_18sp
    )
}