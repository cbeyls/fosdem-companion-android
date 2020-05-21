package be.digitalia.fosdem.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import be.digitalia.fosdem.R
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.utils.enforceSingleScrollDirection
import be.digitalia.fosdem.utils.instantiate
import be.digitalia.fosdem.utils.recyclerView
import be.digitalia.fosdem.utils.viewLifecycleLazy
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy

class TracksFragment : Fragment(R.layout.fragment_tracks), RecycledViewPoolProvider {

    private class ViewHolder(view: View) {
        val contentView: View = view.findViewById(R.id.content)
        val emptyView: View = view.findViewById(android.R.id.empty)
        val pager: ViewPager2 = view.findViewById(R.id.pager)
        val tabs: TabLayout = view.findViewById(R.id.tabs)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val holder = ViewHolder(view).apply {
            pager.apply {
                offscreenPageLimit = 1
                recyclerView.enforceSingleScrollDirection()
            }
        }
        val daysAdapter = DaysAdapter(this)

        var savedCurrentPage = if (savedInstanceState == null) {
            // Restore the current page from preferences
            requireActivity().getPreferences(Context.MODE_PRIVATE).getInt(PREF_CURRENT_PAGE, -1)
        } else -1

        AppDatabase.getInstance(requireContext()).scheduleDao.days.observe(viewLifecycleOwner) { days ->
            holder.run {
                daysAdapter.days = days

                val totalPages = daysAdapter.itemCount
                if (totalPages == 0) {
                    contentView.isVisible = false
                    emptyView.isVisible = true
                } else {
                    contentView.isVisible = true
                    emptyView.isVisible = false
                    if (pager.adapter == null) {
                        pager.adapter = daysAdapter
                        TabLayoutMediator(tabs, pager,
                                TabConfigurationStrategy { tab, position -> tab.text = daysAdapter.getPageTitle(position) }
                        ).attach()
                    }
                    if (savedCurrentPage != -1) {
                        pager.setCurrentItem(savedCurrentPage.coerceAtMost(totalPages - 1), false)
                        savedCurrentPage = -1
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { source, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Save the current page to preferences if it has changed
                val page = holder.pager.currentItem
                val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
                if (prefs.getInt(PREF_CURRENT_PAGE, -1) != page) {
                    prefs.edit {
                        putInt(PREF_CURRENT_PAGE, page)
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
        private const val PREF_CURRENT_PAGE = "tracks_current_page"
    }
}