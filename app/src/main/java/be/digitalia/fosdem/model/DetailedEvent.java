package be.digitalia.fosdem.model;

import android.text.TextUtils;

import java.util.List;

import androidx.annotation.NonNull;

public class DetailedEvent extends Event {

	@NonNull
	private List<Person> persons;
	@NonNull
	private List<Link> links;

	@Override
	@NonNull
	public String getPersonsSummary() {
		return TextUtils.join(", ", persons);
	}

	@NonNull
	public List<Person> getPersons() {
		return persons;
	}

	public void setPersons(@NonNull List<Person> persons) {
		this.persons = persons;
	}

	@NonNull
	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(@NonNull List<Link> links) {
		this.links = links;
	}
}
