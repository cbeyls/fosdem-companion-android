package be.digitalia.fosdem.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.Person;

public class PersonsViewModel extends AndroidViewModel {

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final LiveData<PagedList<Person>> persons
			= new LivePagedListBuilder<>(appDatabase.getScheduleDao().getPersons(), 100)
			.build();

	public PersonsViewModel(@NonNull Application application) {
		super(application);
	}

	public LiveData<PagedList<Person>> getPersons() {
		return persons;
	}
}
