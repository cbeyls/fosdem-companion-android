package be.digitalia.fosdem.fragments;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.db.DatabaseManager;
import be.digitalia.fosdem.loaders.LocalCacheLoader;
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
	private ViewHolder holder;

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

		// Set the persons summary text first; replace it with the clickable text when the loader completes
		holder.personsTextView = (TextView) view.findViewById(R.id.persons);
		holder.personsTextView.setText(event.getPersonsSummary());

		((TextView) view.findViewById(R.id.track)).setText(event.getTrack().getName());
		Date startTime = event.getStartTime();
		Date endTime = event.getEndTime();
		text = String.format("%1$s, %2$s - %3$s", event.getDay().toString(), (startTime != null) ? TIME_DATE_FORMAT.format(startTime) : "?",
				(endTime != null) ? TIME_DATE_FORMAT.format(endTime) : "?");
		((TextView) view.findViewById(R.id.time)).setText(text);
		((TextView) view.findViewById(R.id.room)).setText(event.getRoomName());

		MovementMethod movementMethod = LinkMovementMethod.getInstance();
		textView = (TextView) view.findViewById(R.id.abstract_text);
		text = event.getAbstractText();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(StringUtils.trimEnd(Html.fromHtml(text)));
			textView.setMovementMethod(movementMethod);
		}
		textView = (TextView) view.findViewById(R.id.description);
		text = event.getDescription();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(StringUtils.trimEnd(Html.fromHtml(text)));
			textView.setMovementMethod(movementMethod);
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().initLoader(EVENT_DETAILS_LOADER_ID, null, eventDetailsLoaderCallbacks);
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
					view = holder.inflater.inflate(R.layout.list_divider, holder.linksContainer, false);
					holder.linksContainer.addView(view);
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
			// TODO Launch the person details activity
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