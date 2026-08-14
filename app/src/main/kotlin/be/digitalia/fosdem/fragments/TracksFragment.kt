package be.digitalia.fosdem.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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

@AndroidEntryPoint
class TracksFragment : Fragment(R.layout.fragment_tracks), RecycledViewPoolProvider {

    private class ViewHolder(view: View) {
        val contentView: View = view.findViewById(R.id.content)
        val emptyView: View = view.findViewById(android.R.id.empty)
        val pager: ViewPager2 = view.findViewById(R.id.pager)
        val tabs: TabLayout = view.findViewById(R.id.tabs)
    }

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

        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            try {
                viewModel.state.collect { state ->
                    with(holder) {
                        daysAdapter.days = state.days

                        if (state.days.isEmpty()) {
                            contentView.isVisible = false
                            emptyView.isVisible = true
                        } else {
                            contentView.isVisible = true
                            emptyView.isVisible = false
                            if (pager.adapter == null) {
                                pager.adapter = daysAdapter
                                pager.setCurrentItem(state.initialPage, false)
                                TabLayoutMediator(tabs, pager) { tab, position ->
                                    tab.text = daysAdapter.getPageTitle(position)
                                }.attach()
                            }
                        }
                    }
                }
            } finally {
                // Will be executed when the coroutine block is canceled in onStop()
                with(holder.pager) {
                    if (adapter != null) {
                        viewModel.saveCurrentPage(currentItem)
                    }
                }
            }
        }
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
}