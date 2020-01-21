package be.digitalia.fosdem.utils

import android.os.Parcel
import android.util.LongSparseArray
import kotlinx.android.parcel.Parceler

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
            val size = size()
            parcel.writeInt(size)
            for (i in 0 until size) {
                parcel.writeLong(keyAt(i))
                parcel.writeInt(valueAt(i))
            }
        }
    }
}