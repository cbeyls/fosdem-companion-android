package be.digitalia.fosdem.fragments

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.adapters.ConcatAdapter
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.utils.DateUtils
import be.digitalia.fosdem.utils.configureToolbarColors
import be.digitalia.fosdem.viewmodels.PersonInfoViewModel

class PersonInfoListFragment : RecyclerViewFragment() {

    private val viewModel: PersonInfoViewModel by viewModels()
    private val person by lazy<Person>(LazyThreadSafetyMode.NONE) {
        requireArguments().getParcelable(ARG_PERSON)!!
    }
    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        EventsAdapter(requireContext(), this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.person, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.more_info -> {
            // Look for the first non-placeholder event in the paged list
            val statusEvent = adapter.currentList?.firstOrNull { it != null }
            if (statusEvent != null) {
                val year = DateUtils.getYear(statusEvent.event.day.date.time)
                val url = person.getUrl(year)
                if (url != null) {
                    try {
                        val context = requireContext()
                        CustomTabsIntent.Builder()
                                .configureToolbarColors(context, R.color.light_color_primary)
                                .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                                .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
                                .build()
                                .launchUrl(context, Uri.parse(url))
                    } catch (ignore: ActivityNotFoundException) {
                    }
                }
            }
            true
        }
        else -> false
    }

    override fun onRecyclerViewCreated(recyclerView: RecyclerView, savedInstanceState: Bundle?) = with(recyclerView) {
        val contentMargin = resources.getDimensionPixelSize(R.dimen.content_margin)
        setPadding(contentMargin, contentMargin, contentMargin, contentMargin)
        clipToPadding = false
        scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
        layoutManager = LinearLayoutManager(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setAdapter(ConcatAdapter(HeaderAdapter(), adapter))
        emptyText = getString(R.string.no_data)
        isProgressBarVisible = true

        with(viewModel) {
            setPerson(person)
            events.observe(viewLifecycleOwner) { events ->
                adapter.submitList(events)
                isProgressBarVisible = false
            }
        }
    }

    private class HeaderAdapter : RecyclerView.Adapter<HeaderAdapter.ViewHolder>() {

        override fun getItemCount() = 1

        override fun getItemViewType(position: Int) = R.layout.header_person_info

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.header_person_info, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // Nothing to bind
        }

        private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    companion object {
        private const val ARG_PERSON = "person"

        fun newInstance(person: Person) = PersonInfoListFragment().apply {
            arguments = Bundle(1).apply {
                putParcelable(ARG_PERSON, person)
            }
        }
    }
}