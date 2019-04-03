package be.digitalia.fosdem.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import be.digitalia.fosdem.db.AppDatabase;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.StatusEvent;

public class PersonInfoViewModel extends AndroidViewModel {

	private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
	private final MutableLiveData<Person> person = new MutableLiveData<>();
	private final LiveData<PagedList<StatusEvent>> events = Transformations.switchMap(person,
			person -> new LivePagedListBuilder<>(appDatabase.getScheduleDao().getEvents(person), 20)
					.build());

	public PersonInfoViewModel(@NonNull Application application) {
		super(application);
	}

	public void setPerson(@NonNull Person person) {
		if (!person.equals(this.person.getValue())) {
			this.person.setValue(person);
		}
	}

	public LiveData<PagedList<StatusEvent>> getEvents() {
		return events;
	}
}
