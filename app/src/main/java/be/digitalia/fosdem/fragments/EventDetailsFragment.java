package be.digitalia.fosdem.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import be.digitalia.fosdem.R;
import be.digitalia.fosdem.activities.PersonInfoActivity;
import be.digitalia.fosdem.api.FosdemApi;
import be.digitalia.fosdem.model.Building;
import be.digitalia.fosdem.model.Event;
import be.digitalia.fosdem.model.EventDetails;
import be.digitalia.fosdem.model.Link;
import be.digitalia.fosdem.model.Person;
import be.digitalia.fosdem.model.RoomStatus;
import be.digitalia.fosdem.utils.ClickableArrowKeyMovementMethod;
import be.digitalia.fosdem.utils.CustomTabsUtils;
import be.digitalia.fosdem.utils.DateUtils;
import be.digitalia.fosdem.utils.StringUtils;
import be.digitalia.fosdem.viewmodels.EventDetailsViewModel;

public class EventDetailsFragment extends Fragment {

	static class ViewHolder {
		LayoutInflater inflater;
		TextView personsTextView;
		TextView roomStatus;
		View linksHeader;
		ViewGroup linksContainer;
	}

	private static final String ARG_EVENT = "event";

	Event event;
	ViewHolder holder;
	EventDetailsViewModel viewModel;

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
		event = requireArguments().getParcelable(ARG_EVENT);
		viewModel = new ViewModelProvider(this).get(EventDetailsViewModel.class);
		viewModel.setEvent(event);
		setHasOptionsMenu(true);
	}

	public Event getEvent() {
		return event;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_event_details, container, false);

		holder = new ViewHolder();
		holder.inflater = inflater;

		((TextView) view.findViewById(R.id.title)).setText(event.getTitle());
		TextView textView = view.findViewById(R.id.subtitle);
		String text = event.getSubTitle();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(text);
		}

		// Set the persons summary text first; replace it with the clickable text when the loader completes
		holder.personsTextView = view.findViewById(R.id.persons);
		String personsSummary = event.getPersonsSummary();
		if (TextUtils.isEmpty(personsSummary)) {
			holder.personsTextView.setVisibility(View.GONE);
		} else {
			holder.personsTextView.setText(personsSummary);
			holder.personsTextView.setMovementMethod(LinkMovementMethod.getInstance());
			holder.personsTextView.setVisibility(View.VISIBLE);
		}


		textView = view.findViewById(R.id.time);
		Date startTime = event.getStartTime();
		Date endTime = event.getEndTime();
		DateFormat timeDateFormat = DateUtils.getTimeDateFormat(getActivity());
		text = String.format("%1$s, %2$s ― %3$s",
				event.getDay().toString(),
				(startTime != null) ? timeDateFormat.format(startTime) : "?",
				(endTime != null) ? timeDateFormat.format(endTime) : "?");
		textView.setText(text);
		textView.setContentDescription(getString(R.string.time_content_description, text));

		textView = view.findViewById(R.id.room);
		final String roomName = event.getRoomName();
		Spannable roomText = new SpannableString(String.format("%1$s (Building %2$s)", roomName, Building.fromRoomName(roomName)));
		final int roomImageResId = getResources().getIdentifier(StringUtils.roomNameToResourceName(roomName), "drawable", requireActivity().getPackageName());
		// If the room image exists, make the room text clickable to display it
		if (roomImageResId != 0) {
			roomText.setSpan(new ClickableSpan() {
				@Override
				public void onClick(@NonNull View view) {
					RoomImageDialogFragment.newInstance(roomName, roomImageResId).show(getParentFragmentManager());
				}

				@Override
				public void updateDrawState(@NonNull TextPaint ds) {
					super.updateDrawState(ds);
					ds.setUnderlineText(false);
				}
			}, 0, roomText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
		}
		textView.setText(roomText);
		textView.setContentDescription(getString(R.string.room_content_description, roomText));

		holder.roomStatus = view.findViewById(R.id.room_status);


		textView = view.findViewById(R.id.abstract_text);
		text = event.getAbstractText();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(StringUtils.parseHtml(text, getResources()));
			textView.setMovementMethod(ClickableArrowKeyMovementMethod.getInstance());
		}
		textView = view.findViewById(R.id.description);
		text = event.getDescription();
		if (TextUtils.isEmpty(text)) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(StringUtils.parseHtml(text, getResources()));
			textView.setMovementMethod(ClickableArrowKeyMovementMethod.getInstance());
		}

		holder.linksHeader = view.findViewById(R.id.links_header);
		holder.linksContainer = view.findViewById(R.id.links_container);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		viewModel.getEventDetails().observe(getViewLifecycleOwner(), eventDetails -> {
			if (eventDetails != null) {
				setEventDetails(eventDetails);
			}
		});

		// Live room status
		FosdemApi.getRoomStatuses(requireContext()).observe(getViewLifecycleOwner(), roomStatuses -> {
			RoomStatus roomStatus = roomStatuses.get(event.getRoomName());
			if (roomStatus == null) {
				holder.roomStatus.setText(null);
			} else {
				holder.roomStatus.setText(roomStatus.getNameResId());
				holder.roomStatus.setTextColor(ContextCompat.getColorStateList(requireContext(), roomStatus.getColorResId()));
			}
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		holder = null;
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.event, menu);
		menu.findItem(R.id.share).setIntent(getShareChooserIntent());
	}

	private Intent getShareChooserIntent() {
		return ShareCompat.IntentBuilder.from(requireActivity())
				.setSubject(String.format("%1$s (FOSDEM)", event.getTitle()))
				.setType("text/plain")
				.setText(String.format("%1$s %2$s #FOSDEM", event.getTitle(), event.getUrl()))
				.setChooserTitle(R.string.share)
				.createChooserIntent();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.add_to_agenda:
				addToAgenda();
				return true;
		}
		return false;
	}

	private void addToAgenda() {
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");
		intent.putExtra(CalendarContract.Events.TITLE, event.getTitle());
		intent.putExtra(CalendarContract.Events.EVENT_LOCATION, "ULB - " + event.getRoomName());
		String description = event.getAbstractText();
		if (TextUtils.isEmpty(description)) {
			description = event.getDescription();
		}
		description = StringUtils.stripHtml(description);
		// Add speaker info if available
		EventDetails details = viewModel.getEventDetails().getValue();
		final int personsCount = (details == null) ? 0 : details.getPersons().size();
		if (personsCount > 0) {
			description = String.format("%1$s: %2$s\n\n%3$s", getResources().getQuantityString(R.plurals.speakers, personsCount), event.getPersonsSummary(), description);
		}
		intent.putExtra(CalendarContract.Events.DESCRIPTION, description);
		Date time = event.getStartTime();
		if (time != null) {
			intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, time.getTime());
		}
		time = event.getEndTime();
		if (time != null) {
			intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, time.getTime());
		}
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Snackbar.make(requireView(), R.string.calendar_not_found, Snackbar.LENGTH_LONG).show();
		}
	}

	void setEventDetails(@NonNull EventDetails eventDetails) {
		// 1. Persons
		final List<Person> persons = eventDetails.getPersons();
		if (persons.size() > 0) {
			// Build a list of clickable persons
			SpannableStringBuilder sb = new SpannableStringBuilder();
			int length = 0;
			for (Person person : persons) {
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

		// 2. Links
		final List<Link> links = eventDetails.getLinks();
		holder.linksContainer.removeAllViews();
		if (links.size() > 0) {
			holder.linksHeader.setVisibility(View.VISIBLE);
			holder.linksContainer.setVisibility(View.VISIBLE);
			for (Link link : links) {
				View view = holder.inflater.inflate(R.layout.item_link, holder.linksContainer, false);
				TextView tv = view.findViewById(R.id.description);
				tv.setText(link.getDescription());
				view.setOnClickListener(new LinkClickListener(event, link));
				holder.linksContainer.addView(view);
			}
		} else {
			holder.linksHeader.setVisibility(View.GONE);
			holder.linksContainer.setVisibility(View.GONE);
		}
	}

	private static class PersonClickableSpan extends ClickableSpan {

		private final Person person;

		PersonClickableSpan(Person person) {
			this.person = person;
		}

		@Override
		public void onClick(@NonNull View v) {
			Context context = v.getContext();
			Intent intent = new Intent(context, PersonInfoActivity.class).putExtra(PersonInfoActivity.EXTRA_PERSON, person);
			context.startActivity(intent);
		}

		@Override
		public void updateDrawState(@NonNull TextPaint ds) {
			super.updateDrawState(ds);
			ds.setUnderlineText(false);
		}
	}

	private static class LinkClickListener implements View.OnClickListener {

		private final Event event;
		private final Link link;

		LinkClickListener(@NonNull Event event, @NonNull Link link) {
			this.event = event;
			this.link = link;
		}

		@Override
		public void onClick(View v) {
			String url = link.getUrl();
			try {
				final Context context = v.getContext();
				CustomTabsUtils.configureToolbarColors(new CustomTabsIntent.Builder(), context, event.getTrack().getType().getAppBarColorResId())
						.setShowTitle(true)
						.setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
						.setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
						.build()
						.launchUrl(context, Uri.parse(url));
			} catch (ActivityNotFoundException ignore) {
			}
		}
	}
}