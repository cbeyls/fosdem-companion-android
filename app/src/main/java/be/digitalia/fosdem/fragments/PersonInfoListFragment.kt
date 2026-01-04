package be.digitalia.fosdem.fragments

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.FabOwner
import be.digitalia.fosdem.adapters.EventsAdapter
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.ClickableArrowKeyMovementMethod
import be.digitalia.fosdem.utils.configureColorSchemes
import be.digitalia.fosdem.utils.getParcelableCompat
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.utils.parseHtml
import be.digitalia.fosdem.viewmodels.PersonInfoViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PersonInfoListFragment : Fragment(R.layout.recyclerview) {

    @Inject
    lateinit var userSettingsProvider: UserSettingsProvider
    @Inject
    lateinit var api: FosdemApi
    private val viewModel: PersonInfoViewModel by viewModels(extrasProducer = {
        defaultViewModelCreationExtras.withCreationCallback<PersonInfoViewModel.Factory> { factory ->
            val person: Person = requireArguments().getParcelableCompat(ARG_PERSON)!!
            factory.create(person)
        }
    })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val headerAdapter = HeaderAdapter()
        val eventsAdapter = EventsAdapter(view.context)
        val holder = RecyclerViewViewHolder(view).apply {
            recyclerView.apply {
                val contentMargin = resources.getDimensionPixelSize(R.dimen.content_margin)
                setPadding(contentMargin, contentMargin, contentMargin, contentMargin)
                clipToPadding = false
                scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
                layoutManager = LinearLayoutManager(context)
            }
            val concatAdapterConfig = ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .build()
            setAdapter(ConcatAdapter(concatAdapterConfig, headerAdapter, eventsAdapter))
            isProgressBarVisible = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Hide the progress bar when both the person info and the events list return a first result
            viewModel.personInfo.first()
            eventsAdapter.loadStateFlow.first { it.refresh !is LoadState.Loading }
            holder.isProgressBarVisible = false
        }

        val fab = (requireActivity() as? FabOwner)?.fab
        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            launch {
                viewModel.personInfo.collect { personInfo ->
                    if (fab != null) {
                        configureFabUrl(fab, personInfo.detailsUrl?.toUri())
                    }
                    headerAdapter.biographyHtml = personInfo.biography
                }
            }
            launch {
                userSettingsProvider.timeZoneMode.collect { mode ->
                    eventsAdapter.timeZoneOverride = mode.override
                }
            }
            launch {
                api.roomStatuses.collect { statuses ->
                    eventsAdapter.roomStatuses = statuses
                }
            }
            launch {
                viewModel.events.collectLatest { pagingData ->
                    eventsAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun configureFabUrl(fab: FloatingActionButton, url: Uri?) {
        if (url == null) {
            fab.setOnClickListener(null)
            fab.hide()
        } else {
            fab.setOnClickListener {
                val context = it.context
                try {
                    CustomTabsIntent.Builder()
                        .configureColorSchemes(context, R.color.light_color_primary)
                        .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                        .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
                        .build()
                        .launchUrl(context, url)
                } catch (_: ActivityNotFoundException) {
                }
            }
            fab.show()
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
            holder.bindBiographyHtml(biographyHtml)
        }

        var biographyHtml: String? = null
            set(value) {
                if (field != value) {
                    field = value
                    notifyItemChanged(0)
                }
            }

        private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val biographyTextView: TextView = itemView.findViewById(R.id.biography)

            init {
                biographyTextView.movementMethod = ClickableArrowKeyMovementMethod
            }

            fun bindBiographyHtml(biographyHtml: String?) {
                with(biographyTextView) {
                    if (biographyHtml.isNullOrEmpty()) {
                        text = null
                        isVisible = false
                    } else {
                        text = biographyHtml.parseHtml(resources)
                        isVisible = true
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_PERSON = "person"

        fun createArguments(person: Person) = Bundle(1).apply {
            putParcelable(ARG_PERSON, person)
        }
    }
}