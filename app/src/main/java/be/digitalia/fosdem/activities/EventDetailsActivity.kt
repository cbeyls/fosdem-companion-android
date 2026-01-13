package be.digitalia.fosdem.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import be.digitalia.fosdem.R
import be.digitalia.fosdem.fragments.EventDetailsFragment
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.utils.consumeHorizontalWindowInsetsAsPadding
import be.digitalia.fosdem.utils.getParcelableExtraCompat
import be.digitalia.fosdem.utils.isLightTheme
import be.digitalia.fosdem.utils.rootView
import be.digitalia.fosdem.utils.setTaskColorPrimary
import be.digitalia.fosdem.viewmodels.BookmarkStatusViewModel
import be.digitalia.fosdem.viewmodels.EventViewModel
import be.digitalia.fosdem.widgets.setupBookmarkStatus
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.launch

/**
 * Displays a single event passed either as a complete Parcelable object in extras or as an id in data.
 *
 * @author Christophe Beyls
 */
@AndroidEntryPoint
class EventDetailsActivity : AppCompatActivity(R.layout.single_event) {

    private val bookmarkStatusViewModel: BookmarkStatusViewModel by viewModels()
    private val viewModel: EventViewModel by viewModels(extrasProducer = {
        defaultViewModelCreationExtras.withCreationCallback<EventViewModel.Factory> { factory ->
            // Load the event from the DB using its id
            val eventIdString = intent.dataString!!
            factory.create(eventIdString.toLong())
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        rootView.consumeHorizontalWindowInsetsAsPadding()
        setSupportActionBar(findViewById(R.id.bottom_appbar))

        // Shift the main content up according to insets, since it's covered by the bottom navigation
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content)) { v, insets ->
            val padding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(padding.left, 0, padding.right, padding.bottom)
            // Note: we don't need to manually dispatch the insets to siblings in older Android versions
            // because the content view is a FragmentContainerView which always returns the original insets,
            // even if we return CONSUMED here.
            WindowInsetsCompat.CONSUMED
        }

        findViewById<ImageButton>(R.id.fab).setupBookmarkStatus(bookmarkStatusViewModel, this)

        val intentEvent: Event? = intent.getParcelableExtraCompat(EXTRA_EVENT)

        if (intentEvent != null) {
            // The event has been passed as parameter, it can be displayed immediately
            initEvent(intentEvent)
            if (savedInstanceState == null) {
                supportFragmentManager.commit {
                    add<EventDetailsFragment>(R.id.content,
                            args = EventDetailsFragment.createArguments(intentEvent))
                }
            }
        } else {
            lifecycleScope.launch {
                val event = viewModel.event.await()
                withStarted {
                    if (event == null) {
                        // Event not found, quit
                        Toast.makeText(this@EventDetailsActivity, getString(R.string.event_not_found_error), Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        initEvent(event)

                        val fm = supportFragmentManager
                        if (fm.findFragmentById(R.id.content) == null) {
                            fm.commit(allowStateLoss = true) {
                                add<EventDetailsFragment>(R.id.content,
                                    args = EventDetailsFragment.createArguments(event))
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Initialize event-related configuration after the event has been loaded.
     */
    private fun initEvent(event: Event) {
        // Enable up navigation only after getting the event details
        val toolbar = findViewById<Toolbar>(R.id.toolbar).apply {
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)
            setNavigationOnClickListener { onSupportNavigateUp() }
            title = event.track.name
        }

        val trackType = event.track.type
        if (isLightTheme) {
            val trackAppBarColor = ContextCompat.getColorStateList(this, trackType.appBarColorResId)!!
            setTaskColorPrimary(trackAppBarColor.defaultColor)
            findViewById<AppBarLayout>(R.id.appbar).apply {
                backgroundTintList = trackAppBarColor
                statusBarForeground = getDrawable(trackType.statusBarColorResId)
            }
        } else {
            val trackTextColor = ContextCompat.getColorStateList(this, trackType.textColorResId)!!
            toolbar.setTitleTextColor(trackTextColor)
        }

        bookmarkStatusViewModel.event = event
    }

    override fun getSupportParentActivityIntent(): Intent? {
        val event = bookmarkStatusViewModel.event ?: return null
        // Navigate up to the track associated with this event
        return Intent(this, TrackScheduleActivity::class.java)
                .putExtra(TrackScheduleActivity.EXTRA_DAY, event.day)
                .putExtra(TrackScheduleActivity.EXTRA_TRACK, event.track)
                .putExtra(TrackScheduleActivity.EXTRA_FROM_EVENT_ID, event.id)
    }

    override fun supportNavigateUpTo(upIntent: Intent) {
        // Replicate the compatibility implementation of NavUtils.navigateUpTo()
        // to ensure the parent Activity is always launched
        // even if not present on the back stack.
        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(upIntent)
        finish()
    }

    companion object {
        const val EXTRA_EVENT = "event"
    }
}