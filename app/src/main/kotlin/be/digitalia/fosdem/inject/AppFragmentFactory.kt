package be.digitalia.fosdem.inject

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass

/**
 * A [FragmentFactory] that uses a map of [KClass] to a provider of [Fragment] to create Fragments.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class AppFragmentFactory(private val creators: Map<KClass<out Fragment>, () -> Fragment>) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragmentClass = loadFragmentClass(classLoader, className)
        val creator = creators[fragmentClass.kotlin] ?: return super.instantiate(classLoader, className)

        return try {
            creator()
        } catch (e: Exception) {
            throw RuntimeException("Error creating fragment $className", e)
        }
    }
}
