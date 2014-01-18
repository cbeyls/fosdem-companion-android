package be.digitalia.fosdem.fragments;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.PersonInfoActivity;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.LocalCacheLoader;
import be.digitalia.fosdem.model.Building;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.utils.DateUtils;
import be.digitalia.fosdem.utils.StringUtils;

public class EventDetailsFragment extends Fragment {

	private static class EventDetails {
		List<Person> persons;
		List<Link> links;
	}

	private static class ViewHolder {
		LayoutInflater inflater;
		TextView personsTextView;
		ViewGroup linksContainer;
	}

	private static final int EVENT_DETAILS_LOADER_ID = 1;

	private static final String ARG_EVENT = "event";

	private static final DateFormat TIME_DATE_FORMAT = DateUtils.getTimeDateFormat();

	private Event event;
	private int personsCount = 1;
	private boolean isBookmarked = false;
	private ViewHolder holder;
	private boolean bookmarksChanged = false;

	private final BroadcastReceiver bookmarksReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			bookmarksChanged = true;
		}
	};

	public static EventDetailsFragment newInstance(Event event) {
		EventDetailsFragment f = new EventDetailsFragment();
		Bundle args = new Bundle();
		args.putParcelable(ARG_EVENT, event);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		event = getArguments().getParcelable(ARG_EVENT);
		isBookmarked = DatabaseManager.getInstance().isBookmarked(event);
		setHasOptionsMenu(true);

		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
		lbm.registerReceiver(bookmarksReceiver, new IntentFilter(DatabaseManager.ACTION_ADD_BOOKMARK));
		lbm.registerReceiver(bookmarksReceiver, new IntentFilter(DatabaseManager.ACTION_REMOVE_BOOKMARKS));
	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(bookmarksReceiver);
		super.onDestroy();
	}

	public Event getEvent() {
		return event;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_event_details, container, false);

		holder = new ViewHolder();
		holder.inflater = inflater;

		((TextView) view.findViewById(R.id.title)).setText(event.getTitle());
		TextView textView = (TextView) view.findViewById(R.id.subtitle);
		String text = event.getSubTitle();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(text);
		}

		MovementMethod linkMovementMethod = LinkMovementMethod.getInstance();

		// Set the persons summary text first; replace it with the clickable text when the loader completes
		holder.personsTextView = (TextView) view.findViewById(R.id.persons);
		String personsSummary = event.getPersonsSummary();
		if (TextUtils.isEmpty(personsSummary)) {
			holder.personsTextView.setVisibility(View.GONE);
		} else {
			holder.personsTextView.setText(personsSummary);
			holder.personsTextView.setMovementMethod(linkMovementMethod);
			holder.personsTextView.setVisibility(View.VISIBLE);
		}

		((TextView) view.findViewById(R.id.track)).setText(event.getTrack().getName());
		Date startTime = event.getStartTime();
		Date endTime = event.getEndTime();
		text = String.format("%1$s, %2$s - %3$s", event.getDay().toString(), (startTime != null) ? TIME_DATE_FORMAT.format(startTime) : "?",
				(endTime != null) ? TIME_DATE_FORMAT.format(endTime) : "?");
		((TextView) view.findViewById(R.id.time)).setText(text);
		final String roomName = event.getRoomName();
		TextView roomTextView = (TextView) view.findViewById(R.id.room);
		Spannable roomText = new SpannableString(String.format("%1$s (Building %2$s)", roomName, Building.fromRoomName(roomName)));
		final int roomImageResId = getResources().getIdentifier(StringUtils.roomNameToResourceName(roomName), "drawable", getActivity().getPackageName());
		// If the room image exists, make the room text clickable to display it
		if (roomImageResId != 0) {
			roomText.setSpan(new UnderlineSpan(), 0, roomText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			roomTextView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					RoomImageDialogFragment.newInstance(roomName, roomImageResId).show(getFragmentManager());
				}
			});
			roomTextView.setFocusable(true);
		}
		roomTextView.setText(roomText);

		textView = (TextView) view.findViewById(R.id.abstract_text);
		text = event.getAbstractText();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(StringUtils.trimEnd(Html.fromHtml(text)));
			textView.setMovementMethod(linkMovementMethod);
		}
		textView = (TextView) view.findViewById(R.id.description);
		text = event.getDescription();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(StringUtils.trimEnd(Html.fromHtml(text)));
			textView.setMovementMethod(linkMovementMethod);
		}

		holder.linksContainer = (ViewGroup) view.findViewById(R.id.links_container);
		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		holder = null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.event, menu);

		ShareCompat.configureMenuItem(menu, R.id.share, getShareIntentBuilder());

		MenuItem item = menu.findItem(R.id.bookmark);
		if (isBookmarked) {
			item.setTitle(R.string.remove_bookmark);
			item.setIcon(R.drawable.ic_action_important);
		} else {
			item.setTitle(R.string.add_bookmark);
			item.setIcon(R.drawable.ic_action_not_important);
		}
	}

	private void invalidateOptionsMenu() {
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.bookmark:
			if (!isBookmarked) {
				if (DatabaseManager.getInstance().addBookmark(event)) {
					isBookmarked = true;
					invalidateOptionsMenu();
				}
			} else {
				if (DatabaseManager.getInstance().removeBookmark(event)) {
					isBookmarked = false;
					invalidateOptionsMenu();
				}
			}
			break;
		case R.id.add_to_agenda:
			addToAgenda();
			return true;
		}
		return false;
	}

	private ShareCompat.IntentBuilder getShareIntentBuilder() {
		return ShareCompat.IntentBuilder.from(getActivity()).setSubject(String.format("%1$s (FOSDEM)", event.getTitle())).setType("text/plain")
				.setText(String.format("%1$s %2$s #fosdem", event.getTitle(), event.getUrl())).setChooserTitle(R.string.share);
	}

	@SuppressLint("InlinedApi")
	private void addToAgenda() {
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");
		intent.putExtra(CalendarContract.Events.TITLE, event.getTitle());
		intent.putExtra(CalendarContract.Events.EVENT_LOCATION, "ULB - " + event.getRoomName());
		String description = event.getAbstractText();
		if (TextUtils.isEmpty(description)) {
			description = event.getDescription();
		}
		// Strip HTML
		description = StringUtils.trimEnd(Html.fromHtml(description)).toString();
		// Add speaker info if available
		if (personsCount > 0) {
			description = String.format("%1$s: %2$s\n\n%3$s", getResources().getQuantityString(R.plurals.speakers, personsCount), event.getPersonsSummary(),
					description);
		}
		intent.putExtra(CalendarContract.Events.DESCRIPTION, description);
		intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getStartTime().getTime());
		intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getEndTime().getTime());
		startActivity(intent);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().initLoader(EVENT_DETAILS_LOADER_ID, null, eventDetailsLoaderCallbacks);
	}

	@Override
	public void onStart() {
		super.onStart();

		// If bookmarks have changed while this fragment was stopped, check again if this event is bookmarked
		if (bookmarksChanged) {
			boolean result = DatabaseManager.getInstance().isBookmarked(event);
			if (result != isBookmarked) {
				isBookmarked = result;
				invalidateOptionsMenu();
			}
			bookmarksChanged = false;
		}
	}

	private static class EventDetailsLoader extends LocalCacheLoader<EventDetails> {

		private final Event event;

		public EventDetailsLoader(Context context, Event event) {
			super(context);
			this.event = event;
		}

		@Override
		public EventDetails loadInBackground() {
			EventDetails result = new EventDetails();
			DatabaseManager dbm = DatabaseManager.getInstance();
			result.persons = dbm.getPersons(event);
			result.links = dbm.getLinks(event);
			return result;
		}
	}

	private final LoaderCallbacks<EventDetails> eventDetailsLoaderCallbacks = new LoaderCallbacks<EventDetails>() {
		@Override
		public Loader<EventDetails> onCreateLoader(int id, Bundle args) {
			return new EventDetailsLoader(getActivity(), event);
		}

		@Override
		public void onLoadFinished(Loader<EventDetails> loader, EventDetails data) {
			// 1. Persons
			if (data.persons != null) {
				personsCount = data.persons.size();
				if (personsCount > 0) {
					// Build a list of clickable persons
					SpannableStringBuilder sb = new SpannableStringBuilder();
					int length = 0;
					for (Person person : data.persons) {
						if (length != 0) {
							sb.append(", ");
						}
						String name = person.getName();
						sb.append(name);
						length = sb.length();
						sb.setSpan(new PersonClickableSpan(person), length - name.length(), length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					holder.personsTextView.setText(sb);
					holder.personsTextView.setVisibility(View.VISIBLE);
				}
			}

			// 2. Links
			// Keep the first 2 views in links container (titles) only
			int linkViewCount = holder.linksContainer.getChildCount();
			if (linkViewCount > 2) {
				holder.linksContainer.removeViews(2, linkViewCount - 2);
			}
			if ((data.links != null) && (data.links.size() > 0)) {
				holder.linksContainer.setVisibility(View.VISIBLE);
				for (Link link : data.links) {
					View view = holder.inflater.inflate(R.layout.item_link, holder.linksContainer, false);
					TextView tv = (TextView) view.findViewById(R.id.description);
					tv.setText(link.getDescription());
					view.setOnClickListener(new LinkClickListener(link));
					holder.linksContainer.addView(view);
					// Add a list divider
					holder.inflater.inflate(R.layout.list_divider, holder.linksContainer, true);
				}
			} else {
				holder.linksContainer.setVisibility(View.GONE);
			}
		}

		@Override
		public void onLoaderReset(Loader<EventDetails> loader) {
		}
	};

	private static class PersonClickableSpan extends ClickableSpan {

		private final Person person;

		public PersonClickableSpan(Person person) {
			this.person = person;
		}

		@Override
		public void onClick(View v) {
			Context context = v.getContext();
			Intent intent = new Intent(context, PersonInfoActivity.class).putExtra(PersonInfoActivity.EXTRA_PERSON, person);
			context.startActivity(intent);
		}
	}

	private static class LinkClickListener implements View.OnClickListener {

		private final Link link;

		public LinkClickListener(Link link) {
			this.link = link;
		}

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.getUrl()));
			v.getContext().startActivity(intent);
		}
	}
}