package be.digitalia.fosdem.activities

import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject

/**
 * AppCompatActivity base class which initializes the FragmentFactory and default ViewModelProvider.Factory
 * with injected instances.
 */
@HasMemberInjections
open class MetroAppCompatActivity(@LayoutRes contentLayoutId: Int) : AppCompatActivity(contentLayoutId) {

    @Inject
    private fun setupFragmentFactory(fragmentFactory: FragmentFactory) {
        supportFragmentManager.fragmentFactory = fragmentFactory
    }

    @Inject
    final override lateinit var defaultViewModelProviderFactory: ViewModelProvider.Factory
        private set
}
