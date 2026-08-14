package be.digitalia.fosdem.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.TrackScheduleActivity
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Track
import be.digitalia.fosdem.utils.asyncDifferConfig
import be.digitalia.fosdem.utils.getParcelableCompat
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.viewmodels.TracksListViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback

@AndroidEntryPoint
class TracksListFragment : Fragment(R.layout.recyclerview) {

    private val viewModel: TracksListViewModel by viewModels(extrasProducer = {
        defaultViewModelCreationExtras.withCreationCallback<TracksListViewModel.Factory> { factory ->
            factory.create(day)
        }
    })

    private val day: Day by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments().getParcelableCompat(ARG_DAY)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TracksAdapter(day)
        val holder = RecyclerViewViewHolder(view).apply {
            recyclerView.apply {
                val parent = parentFragment
                if (parent is RecycledViewPoolProvider) {
                    setRecycledViewPool(parent.recycledViewPool)
                }

                layoutManager = LinearLayoutManager(context)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
            setAdapter(adapter)
            emptyText = getString(R.string.no_data)
            isProgressBarVisible = true
        }

        viewLifecycleOwner.launchAndRepeatOnLifecycle {
            viewModel.tracks.collect { tracks ->
                adapter.submitList(tracks)
                holder.isProgressBarVisible = false
            }
        }
    }

    private class TracksAdapter(private val day: Day) :
        ListAdapter<Track, TrackViewHolder>(asyncDifferConfig(DIFF_CALLBACK)) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.simple_list_item_2_material, parent, false)
            return TrackViewHolder(view)
        }

        override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
            holder.bind(day, getItem(position))
        }

        companion object {
            private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Track>() {
                override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
                    // Tracks are identified by name and type only, so contents are automatically the same
                    return true
                }
            }
        }
    }

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val name: TextView = itemView.findViewById(android.R.id.text1)
        val type: TextView = itemView.findViewById(android.R.id.text2)

        var day: Day? = null
        var track: Track? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(day: Day, track: Track) {
            this.day = day
            this.track = track
            name.text = track.name
            type.setText(track.type.nameResId)
            type.setTextColor(ContextCompat.getColorStateList(type.context, track.type.textColorResId))
        }

        override fun onClick(view: View) {
            val day = this.day
            val track = this.track
            if (day != null && track != null) {
                val context = view.context
                val intent = Intent(context, TrackScheduleActivity::class.java)
                        .putExtra(TrackScheduleActivity.EXTRA_DAY, day)
                        .putExtra(TrackScheduleActivity.EXTRA_TRACK, track)
                context.startActivity(intent)
            }
        }
    }

    companion object {
        private const val ARG_DAY = "day"

        fun createArguments(day: Day) = Bundle(1).apply {
            putParcelable(ARG_DAY, day)
        }
    }
}