package be.digitalia.fosdem.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.PersonInfoActivity;
import be.digitalia.fosdem.adapters.SimpleItemCallback;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.viewmodels.PersonsViewModel;

public class PersonsListFragment extends RecyclerViewFragment implements Observer<PagedList<Person>> {

	private PersonsAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new PersonsAdapter();
	}

	@NonNull
	@Override
	protected RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
		return (RecyclerView) inflater.inflate(R.layout.recyclerview_fastscroll, container, false);
	}

	@Override
	protected void onRecyclerViewCreated(RecyclerView recyclerView, Bundle savedInstanceState) {
		recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
		recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setAdapter(adapter);
		setEmptyText(getString(R.string.no_data));
		setProgressBarVisible(true);

		final PersonsViewModel viewModel = new ViewModelProvider(this).get(PersonsViewModel.class);
		viewModel.getPersons().observe(getViewLifecycleOwner(), this);
	}

	@Override
	public void onChanged(PagedList<Person> persons) {
		adapter.submitList(persons);
		setProgressBarVisible(false);
	}

	private static class PersonsAdapter extends PagedListAdapter<Person, PersonViewHolder> {

		private static final DiffUtil.ItemCallback<Person> DIFF_CALLBACK = new SimpleItemCallback<Person>() {
			@Override
			public boolean areContentsTheSame(@NonNull Person oldItem, @NonNull Person newItem) {
				return ObjectsCompat.equals(oldItem.getName(), newItem.getName());
			}
		};

		PersonsAdapter() {
			super(DIFF_CALLBACK);
		}

		@NonNull
		@Override
		public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_item_1_material, parent, false);
			return new PersonViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
			final Person person = getItem(position);
			if (person == null) {
				holder.clear();
			} else {
				holder.bind(person);
			}
		}
	}

	static class PersonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		final TextView textView;

		Person person;

		PersonViewHolder(@NonNull View itemView) {
			super(itemView);
			textView = itemView.findViewById(android.R.id.text1);
			itemView.setOnClickListener(this);
		}

		void clear() {
			this.person = null;
			textView.setText(null);
		}

		void bind(@NonNull Person person) {
			this.person = person;
			textView.setText(person.getName());
		}

		@Override
		public void onClick(View view) {
			if (person != null) {
				final Context context = view.getContext();
				Intent intent = new Intent(context, PersonInfoActivity.class)
						.putExtra(PersonInfoActivity.EXTRA_PERSON, person);
				context.startActivity(intent);
			}
		}
	}
}
