package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.TrackScheduleActivity;
import be.digitalia.fosdem.adapters.SimpleItemCallback;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Track;
import be.digitalia.fosdem.viewmodels.TracksViewModel;

public class TracksListFragment extends RecyclerViewFragment implements Observer<List<Track>> {

	private static final String ARG_DAY = "day";

	private Day day;
	private TracksAdapter adapter;

	public static TracksListFragment newInstance(Day day) {
		TracksListFragment f = new TracksListFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_DAY, day);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		day = requireArguments().getParcelable(ARG_DAY);
		adapter = new TracksAdapter(day);
	}

	@Override
	protected void onRecyclerViewCreated(RecyclerView recyclerView, Bundle savedInstanceState) {
		Fragment parentFragment = getParentFragment();
		if (parentFragment instanceof RecycledViewPoolProvider) {
			recyclerView.setRecycledViewPool(((RecycledViewPoolProvider) parentFragment).getRecycledViewPool());
		}

		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
		recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setAdapter(adapter);
		setEmptyText(getString(R.string.no_data));
		setProgressBarVisible(true);

		final TracksViewModel viewModel = new ViewModelProvider(this).get(TracksViewModel.class);
		viewModel.setDay(day);
		viewModel.getTracks().observe(getViewLifecycleOwner(), this);
	}

	@Override
	public void onChanged(List<Track> tracks) {
		adapter.submitList(tracks);
		setProgressBarVisible(false);
	}

	private static class TracksAdapter extends ListAdapter<Track, TrackViewHolder> {

		private static final DiffUtil.ItemCallback<Track> DIFF_CALLBACK = new SimpleItemCallback<Track>() {
			@Override
			public boolean areContentsTheSame(@NonNull Track oldItem, @NonNull Track newItem) {
				return oldItem.getName().equals(newItem.getName())
						&& oldItem.getType().equals(newItem.getType());
			}
		};

		private final Day day;

		TracksAdapter(Day day) {
			super(DIFF_CALLBACK);
			this.day = day;
		}

		@NonNull
		@Override
		public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_item_2_material, parent, false);
			return new TrackViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
			holder.bind(day, getItem(position));
		}
	}

	static class TrackViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		final TextView name;
		final TextView type;

		Day day;
		Track track;

		TrackViewHolder(View itemView) {
			super(itemView);
			name = itemView.findViewById(android.R.id.text1);
			type = itemView.findViewById(android.R.id.text2);
			itemView.setOnClickListener(this);
		}

		void bind(@NonNull Day day, @NonNull Track track) {
			this.day = day;
			this.track = track;
			name.setText(track.getName());
			type.setText(track.getType().getNameResId());
			type.setTextColor(ContextCompat.getColorStateList(type.getContext(), track.getType().getTextColorResId()));
		}

		@Override
		public void onClick(View view) {
			Context context = view.getContext();
			Intent intent = new Intent(context, TrackScheduleActivity.class)
					.putExtra(TrackScheduleActivity.EXTRA_DAY, day)
					.putExtra(TrackScheduleActivity.EXTRA_TRACK, track);
			context.startActivity(intent);
		}
	}
}
