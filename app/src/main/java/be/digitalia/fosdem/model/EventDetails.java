package be.digitalia.fosdem.model;

import androidx.annotation.NonNull;

import java.util.List;

public class EventDetails {

	@NonNull
	private final List<Person> persons;
	@NonNull
	private final List<Link> links;

	public EventDetails(@NonNull List<Person> persons, @NonNull List<Link> links) {
		this.persons = persons;
		this.links = links;
	}

	@NonNull
	public List<Person> getPersons() {
		return persons;
	}

	@NonNull
	public List<Link> getLinks() {
		return links;
	}
}
