package be.digitalia.fosdem.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import be.digitalia.fosdem.R
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.utils.enforceSingleScrollDirection
import be.digitalia.fosdem.utils.recyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy

class TracksFragment : Fragment(), RecycledViewPoolProvider {

    private class ViewHolder(view: View, fragment: Fragment) {
        val contentView: View = view.findViewById(R.id.content)
        val emptyView: View = view.findViewById(android.R.id.empty)
        val pager: ViewPager2 = view.findViewById(R.id.pager)
        val tabs: TabLayout = view.findViewById(R.id.tabs)

        val daysAdapter = DaysAdapter(fragment)
        val recycledViewPool = RecycledViewPool()
    }

    private var holder: ViewHolder? = null
    private var savedCurrentPage = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // Restore the current page from preferences
            savedCurrentPage = requireActivity().getPreferences(Context.MODE_PRIVATE).getInt(PREF_CURRENT_PAGE, -1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tracks, container, false)

        holder = ViewHolder(view, this).apply {
            pager.apply {
                offscreenPageLimit = 1
                recyclerView.enforceSingleScrollDirection()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        holder = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        AppDatabase.getInstance(requireContext()).scheduleDao.days.observe(viewLifecycleOwner) { days ->
            holder?.run {
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
    }

    override fun onStop() {
        super.onStop()
        // Save the current page to preferences if it has changed
        val page = holder?.pager?.currentItem ?: -1
        val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
        if (prefs.getInt(PREF_CURRENT_PAGE, -1) != page) {
            prefs.edit {
                putInt(PREF_CURRENT_PAGE, page)
            }
        }
    }

    override val recycledViewPool: RecycledViewPool?
        get() = holder?.recycledViewPool

    private class DaysAdapter(fragment: Fragment)
        : FragmentStateAdapter(fragment.childFragmentManager, fragment.viewLifecycleOwner.lifecycle) {

        var days: List<Day>? = null
            set(value) {
                if (field != value) {
                    field = value
                    notifyDataSetChanged()
                }
            }

        override fun getItemCount() = days?.size ?: 0

        override fun getItemId(position: Int) = days!![position].index.toLong()

        override fun containsItem(itemId: Long): Boolean {
            return days?.any { it.index.toLong() == itemId } ?: false
        }

        override fun createFragment(position: Int) = TracksListFragment.newInstance(days!![position])

        fun getPageTitle(position: Int) = days!![position].toString()
    }

    companion object {
        private const val PREF_CURRENT_PAGE = "tracks_current_page"
    }
}