package be.digitalia.fosdem.utils

import android.os.Parcel
import kotlinx.parcelize.Parceler
import java.time.Instant
import java.time.LocalDate

object InstantParceler : Parceler<Instant?> {

    override fun create(parcel: Parcel): Instant? {
        val nanoAdjustment = parcel.readInt()
        if (nanoAdjustment == Int.MIN_VALUE) {
            return null
        }
        val epochSecond = parcel.readLong()
        return Instant.ofEpochSecond(epochSecond, nanoAdjustment.toLong())
    }

    override fun Instant?.write(parcel: Parcel, flags: Int) {
        if (this == null) {
            parcel.writeInt(Int.MIN_VALUE)
        } else {
            parcel.writeInt(nano)
            parcel.writeLong(epochSecond)
        }
    }
}

object LocalDateParceler : Parceler<LocalDate> {

    override fun create(parcel: Parcel): LocalDate {
        val year = parcel.readInt()
        val monthDay = parcel.readInt()
        return LocalDate.of(year, monthDay shr 16, monthDay and 0xFFFF)
    }

    override fun LocalDate.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(year)
        // pack month and day into a single int
        parcel.writeInt((monthValue shl 16) or dayOfMonth)
    }
}