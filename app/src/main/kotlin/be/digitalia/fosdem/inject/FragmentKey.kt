package be.digitalia.fosdem.inject

import androidx.fragment.app.Fragment
import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

/** A [MapKey] annotation for binding Fragments in a multibinding map. */
@MapKey(implicitClassKey = true)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class FragmentKey(val value: KClass<out Fragment> = Nothing::class)
