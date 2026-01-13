package be.digitalia.fosdem.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.WindowInsetsApplier
import be.digitalia.fosdem.R
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.utils.enforceSingleScrollDirection
import be.digitalia.fosdem.utils.instantiate
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.utils.recyclerView
import be.digitalia.fosdem.utils.viewLifecycleLazy
import be.digitalia.fosdem.viewmodels.TracksViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class TracksFragment : Fragment(R.layout.fragment_tracks), RecycledViewPoolProvider {

    private class ViewHolder(view: View) {
        val contentView: View = view.findViewById(R.id.content)
        val emptyView: View = view.findViewById(android.R.id.empty)
        val pager: ViewPager2 = view.findViewById(R.id.pager)
        val tabs: TabLayout = view.findViewById(R.id.tabs)
    }

    @Inject
    @Named("UIState")
    lateinit var preferences: SharedPreferences

    private val viewModel: TracksViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val holder = ViewHolder(view).apply {
            pager.apply {
                offscreenPageLimit = 1
                WindowInsetsApplier.install(this)
                recyclerView.enforceSingleScrollDirection()
            }
        }
        val daysAdapter = DaysAdapter(this)

        var savedCurrentPage = if (savedInstanceState == null) {
            // Restore the current page from preferences
            preferences.getInt(TRACKS_CURRENT_PAGE_PREF_KEY, -1)
        } else -1

        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            viewModel.days.collect { days ->
                holder.run {
                    daysAdapter.days = days

                    if (days.isEmpty()) {
                        contentView.isVisible = false
                        emptyView.isVisible = true
                    } else {
                        contentView.isVisible = true
                        emptyView.isVisible = false
                        if (pager.adapter == null) {
                            pager.adapter = daysAdapter
                            TabLayoutMediator(tabs, pager) { tab, position -> tab.text = daysAdapter.getPageTitle(position) }.attach()
                        }
                        if (savedCurrentPage != -1) {
                            pager.setCurrentItem(savedCurrentPage.coerceAtMost(days.size - 1), false)
                            savedCurrentPage = -1
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Save the current page to preferences if it has changed
                val page = holder.pager.currentItem
                if (preferences.getInt(TRACKS_CURRENT_PAGE_PREF_KEY, -1) != page) {
                    preferences.edit {
                        putInt(TRACKS_CURRENT_PAGE_PREF_KEY, page)
                    }
                }
            }
        })
    }

    override val recycledViewPool by viewLifecycleLazy {
        RecyclerView.RecycledViewPool()
    }

    private class DaysAdapter(fragment: Fragment)
        : FragmentStateAdapter(fragment.childFragmentManager, fragment.viewLifecycleOwner.lifecycle) {
        private val fragmentFactory = fragment.childFragmentManager.fragmentFactory

        var days: List<Day> = emptyList()
            set(value) {
                if (field != value) {
                    field = value
                    notifyDataSetChanged()
                }
            }

        override fun getItemCount() = days.size

        override fun getItemId(position: Int) = days[position].index.toLong()

        override fun containsItem(itemId: Long): Boolean {
            return days.any { it.index.toLong() == itemId }
        }

        override fun createFragment(position: Int) = fragmentFactory.instantiate<TracksListFragment>().apply {
            arguments = TracksListFragment.createArguments(days[position])
        }

        fun getPageTitle(position: Int) = days[position].toString()
    }

    companion object {
        private const val TRACKS_CURRENT_PAGE_PREF_KEY = "tracks_current_page"
    }
}