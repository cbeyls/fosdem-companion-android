package be.digitalia.fosdem.utils

import android.os.Parcel
import android.util.LongSparseArray
import androidx.core.util.forEach
import androidx.core.util.size
import kotlinx.android.parcel.Parceler
import java.util.*

object DateParceler : Parceler<Date?> {

    override fun create(parcel: Parcel): Date? {
        val value = parcel.readLong()
        return if (value == -1L) null else Date(value)
    }

    override fun Date?.write(parcel: Parcel, flags: Int) = parcel.writeLong(this?.time ?: -1L)
}

object IntLongSparseArrayParceler : Parceler<LongSparseArray<Int>?> {

    override fun create(parcel: Parcel): LongSparseArray<Int>? {
        val size = parcel.readInt()
        return if (size >= 0) {
            LongSparseArray<Int>(size).apply {
                for (i in 0 until size) {
                    val key = parcel.readLong()
                    val value = parcel.readInt()
                    append(key, value)
                }
            }
        } else null
    }

    override fun LongSparseArray<Int>?.write(parcel: Parcel, flags: Int) {
        if (this == null) {
            parcel.writeInt(-1)
        } else {
            parcel.writeInt(size)
            forEach { key, value ->
                parcel.writeLong(key)
                parcel.writeInt(value)
            }
        }
    }
}