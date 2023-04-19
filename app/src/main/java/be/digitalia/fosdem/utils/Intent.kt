package be.digitalia.fosdem.utils

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String?): T? =
    IntentCompat.getParcelableExtra(this, key, T::class.java)

inline fun <reified T : Parcelable> Intent.getParcelableArrayExtraCompat(key: String?): Array<Parcelable>? =
    IntentCompat.getParcelableArrayExtra(this, key, T::class.java)