package be.digitalia.fosdem.utils

import android.os.Parcel
import kotlinx.parcelize.Parceler
import java.util.Date

object DateParceler : Parceler<Date?> {

    override fun create(parcel: Parcel): Date? {
        val value = parcel.readLong()
        return if (value == -1L) null else Date(value)
    }

    override fun Date?.write(parcel: Parcel, flags: Int) = parcel.writeLong(this?.time ?: -1L)
}