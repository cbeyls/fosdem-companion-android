package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.TrackScheduleActivity;
import be.digitalia.fosdem.adapters.RecyclerViewCursorAdapter;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.SimpleCursorLoader;
import be.digitalia.fosdem.model.Day;
import be.digitalia.fosdem.model.Track;

public class TracksListFragment extends RecyclerViewFragment implements LoaderCallbacks<Cursor> {

	private static final int TRACKS_LOADER_ID = 1;
	private static final String ARG_DAY = "day";

	Day day;
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
		adapter = new TracksAdapter();
		day = getArguments().getParcelable(ARG_DAY);
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

		LoaderManager.getInstance(this).initLoader(TRACKS_LOADER_ID, null, this);
	}

	private static class TracksLoader extends SimpleCursorLoader {

		private final Day day;

		public TracksLoader(Context context, Day day) {
			super(context);
			this.day = day;
		}

		@Override
		protected Cursor getCursor() {
			return DatabaseManager.getInstance().getTracks(day);
		}
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new TracksLoader(getActivity(), day);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
		if (data != null) {
			adapter.swapCursor(data);
		}

		setProgressBarVisible(false);
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	private class TracksAdapter extends RecyclerViewCursorAdapter<TrackViewHolder> {

		private final LayoutInflater inflater;

		public TracksAdapter() {
			inflater = LayoutInflater.from(getContext());
		}

		@Override
		public Track getItem(int position) {
			return DatabaseManager.toTrack((Cursor) super.getItem(position));
		}

		@NonNull
		@Override
		public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.simple_list_item_2_material, parent, false);
			return new TrackViewHolder(view);
		}

		@Override
		public void onBindViewHolder(TrackViewHolder holder, Cursor cursor) {
			holder.day = day;
			holder.track = DatabaseManager.toTrack(cursor, holder.track);
			holder.name.setText(holder.track.getName());
			holder.type.setText(holder.track.getType().getNameResId());
			holder.type.setTextColor(ContextCompat.getColor(holder.type.getContext(), holder.track.getType().getColorResId()));
		}
	}

	static class TrackViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		TextView name;
		TextView type;

		Day day;
		Track track;

		TrackViewHolder(View itemView) {
			super(itemView);
			name = itemView.findViewById(android.R.id.text1);
			type = itemView.findViewById(android.R.id.text2);
			itemView.setOnClickListener(this);
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
