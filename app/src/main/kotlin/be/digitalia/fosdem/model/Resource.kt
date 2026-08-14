package be.digitalia.fosdem.model

import android.os.Parcelable

interface Resource : Parcelable {
    val url: String
    val description: String?
}