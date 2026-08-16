package be.digitalia.fosdem.inject

import android.content.Context

interface AndroidAppGraphProvider {
    val appGraph: AndroidAppGraph
}

val Context.appGraph: AndroidAppGraph
    get() = (applicationContext as AndroidAppGraphProvider).appGraph
