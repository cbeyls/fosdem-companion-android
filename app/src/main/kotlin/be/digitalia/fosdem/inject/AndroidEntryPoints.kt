package be.digitalia.fosdem.inject

import android.content.Context
import androidx.fragment.app.FragmentActivity
import be.digitalia.fosdem.FosdemApplication

val Context.appGraph : AndroidAppGraph
    get() = (applicationContext as FosdemApplication).appGraph

fun FragmentActivity.setupMetroFragmentFactory(): AndroidAppGraph {
    return appGraph.also {
        supportFragmentManager.fragmentFactory = it.fragmentFactory
    }
}
