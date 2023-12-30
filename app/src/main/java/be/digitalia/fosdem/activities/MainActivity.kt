package be.digitalia.fosdem.activities

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.R
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.fragments.BookmarksListFragment
import be.digitalia.fosdem.fragments.LiveFragment
import be.digitalia.fosdem.fragments.MapFragment
import be.digitalia.fosdem.fragments.PersonsListFragment
import be.digitalia.fosdem.fragments.TracksFragment
import be.digitalia.fosdem.model.DownloadScheduleResult
import be.digitalia.fosdem.model.LoadingState
import be.digitalia.fosdem.utils.awaitCloseDrawer
import be.digitalia.fosdem.utils.configureToolbarColors
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.utils.toLocalDateTime
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named

/**
 * Main entry point of the application. Allows to switch between section fragments and update the database.
 *
 * @author Christophe Beyls
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.main) {

    private enum class Section(val fragmentClass: Class<out Fragment>,
                               @IdRes @get:IdRes val menuItemId: Int,
                               val extendsAppBar: Boolean,
                               val keep: Boolean) {
        TRACKS(TracksFragment::class.java, R.id.menu_tracks, true, true),
        BOOKMARKS(BookmarksListFragment::class.java, R.id.menu_bookmarks, false, false),
        LIVE(LiveFragment::class.java, R.id.menu_live, true, false),
        SPEAKERS(PersonsListFragment::class.java, R.id.menu_speakers, false, false),
        MAP(MapFragment::class.java, R.id.menu_map, false, false);

        companion object {
            fun fromMenuItemId(@IdRes menuItemId: Int): Section? {
                return entries.firstOrNull { it.menuItemId == menuItemId }
            }
        }
    }

    private class ViewHolder(val contentView: View,
                             val drawerLayout: DrawerLayout,
                             val navigationView: NavigationView)

    @Inject
    @Named("UIState")
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var api: FosdemApi

    @Inject
    lateinit var scheduleDao: ScheduleDao

    private val latestUpdateDateTimeFormatter = DateTimeFormatter.ofPattern(LATEST_UPDATE_DATE_TIME_FORMAT)

    private lateinit var holder: ViewHolder
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var currentSection: Section

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))
        val contentView: View = findViewById(R.id.content)
        val progressIndicator: BaseProgressIndicator<*> = findViewById(R.id.progress)

        // Monitor the schedule download
        launchAndRepeatOnLifecycle {
            api.downloadScheduleState.collect { state ->
                when (state) {
                    is LoadingState.Loading -> {
                        with(progressIndicator) {
                            when (val progressValue = state.progress) {
                                -1 -> if (!isIndeterminate) {
                                    isInvisible = true
                                    isIndeterminate = true
                                }
                                else -> setProgressCompat(progressValue, true)
                            }
                            show()
                        }
                    }
                    is LoadingState.Idle -> {
                        with(progressIndicator) {
                            // Fix: stop transitioning to determinate when hiding
                            isIndeterminate = false
                            setProgressCompat(100, false)
                            hide()
                        }

                        state.result?.let { result ->
                            val snackbar = when (result) {
                                is DownloadScheduleResult.Error -> {
                                    Snackbar.make(contentView, R.string.schedule_loading_error, ERROR_MESSAGE_DISPLAY_DURATION)
                                        .setAction(R.string.schedule_loading_retry_action) { api.downloadSchedule() }
                                }
                                is DownloadScheduleResult.UpToDate -> {
                                    Snackbar.make(contentView, R.string.events_download_up_to_date, Snackbar.LENGTH_LONG)
                                }
                                is DownloadScheduleResult.Success -> {
                                    val eventsCount = result.eventsCount
                                    val message = if (eventsCount == 0) {
                                        getString(R.string.events_download_empty)
                                    } else {
                                        resources.getQuantityString(R.plurals.events_download_completed, eventsCount, eventsCount)
                                    }
                                    Snackbar.make(contentView, message, Snackbar.LENGTH_LONG)
                                }
                            }
                            snackbar.addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                                    api.downloadScheduleResultConsumed()
                                }
                            }).show()
                        }
                    }
                }
            }
        }

        // Setup drawer layout
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        // Disable drawerLayout focus to allow trackball navigation.
        // We handle the drawer closing on back press ourselves.
        drawerLayout.isFocusable = false
        drawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, R.string.main_menu, R.string.close_menu) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                // Make keypad navigation easier
                holder.navigationView.requestFocus()
            }
        }.apply {
            isDrawerIndicatorEnabled = true
            drawerLayout.addDrawerListener(this)
        }

        // Setup Main menu
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            lifecycleScope.launch {
                try {
                    drawerLayout.awaitCloseDrawer(navigationView)
                    withStarted {
                        handleNavigationMenuItem(menuItem)
                    }
                } catch (e: CancellationException) {
                    // reset the menu to the current selection
                    navigationView.setCheckedItem(currentSection.menuItemId)
                }
            }
            true
        }

        // Latest update date, below the list
        val latestUpdateTextView: TextView = navigationView.findViewById(R.id.latest_update)
        lifecycleScope.launch {
            scheduleDao.latestUpdateTime.collect { time ->
                val timeString = time?.toLocalDateTime(ZoneId.systemDefault())?.format(latestUpdateDateTimeFormatter)
                        ?: getString(R.string.never)
                latestUpdateTextView.text = getString(R.string.last_update, timeString)
            }
        }

        holder = ViewHolder(contentView, drawerLayout, navigationView)

        if (savedInstanceState == null) {
            // Select initial section
            currentSection = when (intent.action) {
                ACTION_SHORTCUT_BOOKMARKS -> Section.BOOKMARKS
                ACTION_SHORTCUT_LIVE -> Section.LIVE
                else -> Section.TRACKS
            }.also { section ->
                navigationView.setCheckedItem(section.menuItemId)
                supportFragmentManager.commit { add(R.id.content, section.fragmentClass, null, section.name) }
            }
        }
    }

    @SuppressLint("PrivateResource")
    private fun updateActionBar(section: Section, menuItem: MenuItem) {
        title = menuItem.title
        ViewCompat.setTranslationZ(holder.contentView,
                if (section.extendsAppBar) resources.getDimension(com.google.android.material.R.dimen.design_appbar_elevation) else 0f)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()

        // Restore current section from NavigationView
        holder.navigationView.checkedItem?.let { menuItem ->
            if (savedInstanceState != null) {
                currentSection = Section.fromMenuItemId(menuItem.itemId)!!
            }
            updateActionBar(currentSection, menuItem)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Allow software back press to be properly dispatched to drawer layout
        val handled = when (event.action) {
            KeyEvent.ACTION_DOWN -> holder.drawerLayout.onKeyDown(event.keyCode, event)
            KeyEvent.ACTION_UP -> holder.drawerLayout.onKeyUp(event.keyCode, event)
            else -> false
        }
        return handled || super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()

        // Scheduled database update
        lifecycleScope.launch {
            val now = Instant.now()
            val latestUpdateTime = scheduleDao.latestUpdateTime.first()
            if (latestUpdateTime == null || latestUpdateTime < now - DATABASE_VALIDITY_DURATION) {
                val latestAttemptTime = Instant.ofEpochMilli(
                    preferences.getLong(LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY, 0L)
                )
                if (latestAttemptTime == Instant.EPOCH || latestAttemptTime < now - AUTO_UPDATE_SNOOZE_DURATION) {
                    preferences.edit {
                        putLong(LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY, now.toEpochMilli())
                    }
                    // Try to update immediately. If it fails, the user gets a message and a retry button.
                    api.downloadSchedule()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Will close the drawer if the home button is pressed
        if (super.onOptionsItemSelected(item) || drawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        return when (item.itemId) {
            R.id.search -> {
                val intent = Intent(this, SearchResultActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                true
            }
            R.id.refresh -> {
                val icon = item.icon
                if (icon is Animatable) {
                    // Hack: reset the icon to make sure the MenuItem will redraw itself properly
                    item.icon = icon
                    icon.start()
                }
                api.downloadSchedule()
                true
            }
            else -> false
        }
    }

    // MAIN MENU

    private fun handleNavigationMenuItem(menuItem: MenuItem) {
        val menuItemId = menuItem.itemId
        val section = Section.fromMenuItemId(menuItemId)
        if (section != null) {
            selectMenuSection(section, menuItem)
        } else {
            when (menuItemId) {
                R.id.menu_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.partial_zoom_out)
                }
                R.id.menu_volunteer -> try {
                    CustomTabsIntent.Builder()
                            .configureToolbarColors(this, R.color.light_color_primary)
                            .setShowTitle(true)
                            .build()
                            .launchUrl(this, Uri.parse(FosdemUrls.volunteer))
                } catch (ignore: ActivityNotFoundException) {
                }
            }
        }
    }

    private fun selectMenuSection(section: Section, menuItem: MenuItem) {
        if (section != currentSection) {
            // Switch to new section
            val fm = supportFragmentManager
            fm.commit {
                fm.findFragmentById(R.id.content)?.let { currentFragment ->
                    if (currentSection.keep) {
                        detach(currentFragment)
                    } else {
                        remove(currentFragment)
                    }
                }
                val cachedFragment = fm.findFragmentByTag(section.name)
                if (section.keep && cachedFragment != null) {
                    attach(cachedFragment)
                } else {
                    add(R.id.content, section.fragmentClass, null, section.name)
                }
            }

            currentSection = section
            updateActionBar(section, menuItem)
        }
    }

    companion object {
        const val ACTION_SHORTCUT_BOOKMARKS = "${BuildConfig.APPLICATION_ID}.intent.action.SHORTCUT_BOOKMARKS"
        const val ACTION_SHORTCUT_LIVE = "${BuildConfig.APPLICATION_ID}.intent.action.SHORTCUT_LIVE"

        private const val ERROR_MESSAGE_DISPLAY_DURATION = 5000
        private val DATABASE_VALIDITY_DURATION = Duration.ofDays(1L)
        private val AUTO_UPDATE_SNOOZE_DURATION = Duration.ofDays(1L)
        private const val LATEST_UPDATE_ATTEMPT_TIME_PREF_KEY = "latest_update_attempt_time"
        private const val LATEST_UPDATE_DATE_TIME_FORMAT = "d MMM yyyy kk:mm:ss"
    }
}