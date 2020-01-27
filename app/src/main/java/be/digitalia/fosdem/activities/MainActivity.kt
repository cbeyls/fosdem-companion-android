package be.digitalia.fosdem.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.nfc.NdefRecord
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.R
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.fragments.*
import be.digitalia.fosdem.model.DownloadScheduleResult
import be.digitalia.fosdem.utils.CreateNfcAppDataCallback
import be.digitalia.fosdem.utils.awaitCloseDrawer
import be.digitalia.fosdem.utils.configureToolbarColors
import be.digitalia.fosdem.utils.setNfcAppDataPushMessageCallbackIfAvailable
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException

/**
 * Main entry point of the application. Allows to switch between section fragments and update the database.
 *
 * @author Christophe Beyls
 */
class MainActivity : AppCompatActivity(), CreateNfcAppDataCallback {

    private enum class Section(@IdRes @get:IdRes val menuItemId: Int, val extendsAppBar: Boolean, val keep: Boolean) {
        TRACKS(R.id.menu_tracks, true, true) {
            override fun createFragment() = TracksFragment()
        },
        BOOKMARKS(R.id.menu_bookmarks, false, false) {
            override fun createFragment() = BookmarksListFragment()
        },
        LIVE(R.id.menu_live, true, false) {
            override fun createFragment() = LiveFragment()
        },
        SPEAKERS(R.id.menu_speakers, false, false) {
            override fun createFragment() = PersonsListFragment()
        },
        MAP(R.id.menu_map, false, false) {
            override fun createFragment() = MapFragment()
        };

        abstract fun createFragment(): Fragment

        companion object {
            fun fromMenuItemId(@IdRes menuItemId: Int): Section? {
                return values().firstOrNull { it.menuItemId == menuItemId }
            }
        }
    }

    private class ViewHolder(val contentView: View,
                             val drawerLayout: DrawerLayout,
                             val navigationView: NavigationView)

    private lateinit var holder: ViewHolder
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private var searchMenuItem: MenuItem? = null

    private lateinit var currentSection: Section

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        setSupportActionBar(findViewById(R.id.toolbar))
        val contentView: View = findViewById(R.id.content)

        // Progress bar setup
        val progressBar: ProgressBar = findViewById(R.id.progress)
        FosdemApi.downloadScheduleProgress.observe(this) { progressValue ->
            progressBar.apply {
                if (progressValue != 100) {
                    // Visible
                    if (!isVisible) {
                        clearAnimation()
                        isVisible = true
                    }
                    if (progressValue == -1) {
                        isIndeterminate = true
                    } else {
                        isIndeterminate = false
                        progress = progressValue
                    }
                } else {
                    // Invisible
                    if (isVisible) {
                        // Hide the progress bar with a fill and fade out animation
                        isIndeterminate = false
                        progress = 100
                        animate()
                                .alpha(0f)
                                .withLayer()
                                .setListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        isVisible = false
                                        alpha = 1f
                                    }
                                })
                    }
                }
            }
        }

        // Monitor the schedule download result
        FosdemApi.downloadScheduleResult.observe(this) { singleEvent ->
            val result = singleEvent.consume() ?: return@observe
            val snackbar = when (result) {
                is DownloadScheduleResult.Error -> {
                    Snackbar.make(contentView, R.string.schedule_loading_error, ERROR_MESSAGE_DISPLAY_DURATION)
                            .setAction(R.string.schedule_loading_retry_action) { FosdemApi.downloadSchedule(this) }
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
            snackbar.show()
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
            lifecycleScope.launchWhenStarted {
                try {
                    drawerLayout.awaitCloseDrawer(navigationView)
                    handleNavigationMenuItem(menuItem)
                } catch (e: CancellationException) {
                    // reset the menu to the current selection
                    navigationView.setCheckedItem(currentSection.menuItemId)
                }
            }
            true
        }

        // Latest update date, below the list
        val latestUpdateTextView: TextView = navigationView.findViewById(R.id.latest_update)
        AppDatabase.getInstance(this).scheduleDao.latestUpdateTime
                .observe(this) { time ->
                    val timeString = if (time == -1L) getString(R.string.never)
                    else DateFormat.format(LATEST_UPDATE_DATE_FORMAT, time)
                    latestUpdateTextView.text = getString(R.string.last_update, timeString)
                }

        holder = ViewHolder(contentView, drawerLayout, navigationView)

        if (savedInstanceState == null) {
            // Select initial section
            val section = when (intent.action) {
                ACTION_SHORTCUT_BOOKMARKS -> Section.BOOKMARKS
                ACTION_SHORTCUT_LIVE -> Section.LIVE
                else -> Section.TRACKS
            }
            currentSection = section
            navigationView.setCheckedItem(section.menuItemId)

            supportFragmentManager.commit { add(R.id.content, section.createFragment(), section.name) }
        }

        setNfcAppDataPushMessageCallbackIfAvailable(this)
    }

    @SuppressLint("PrivateResource")
    private fun updateActionBar(section: Section, menuItem: MenuItem) {
        title = menuItem.title
        ViewCompat.setTranslationZ(holder.contentView,
                if (section.extendsAppBar) resources.getDimension(R.dimen.design_appbar_elevation) else 0f)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()

        // Restore current section from NavigationView
        if (savedInstanceState != null) {
            holder.navigationView.checkedItem?.let { menuItem ->
                val section = Section.fromMenuItemId(menuItem.itemId)!!
                currentSection = section
                updateActionBar(section, menuItem)
            }
        }
    }

    override fun onBackPressed() {
        if (holder.drawerLayout.isDrawerOpen(holder.navigationView)) {
            holder.drawerLayout.closeDrawer(holder.navigationView)
        } else {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (delegate.applyDayNight()) {
                recreate()
            }
        }
        super.onStart()

        // Scheduled database update
        val now = System.currentTimeMillis()
        val latestUpdateTime = AppDatabase.getInstance(this).scheduleDao.latestUpdateTime.value
                ?: -1L
        if (latestUpdateTime == -1L || latestUpdateTime < now - DATABASE_VALIDITY_DURATION) {
            val prefs = getPreferences(Context.MODE_PRIVATE)
            val latestAttemptTime = prefs.getLong(PREF_LATEST_AUTO_UPDATE_ATTEMPT_TIME, -1L)
            if (latestAttemptTime == -1L || latestAttemptTime < now - AUTO_UPDATE_SNOOZE_DURATION) {
                prefs.edit {
                    putLong(PREF_LATEST_AUTO_UPDATE_ATTEMPT_TIME, now)
                }
                // Try to update immediately. If it fails, the user gets a message and a retry button.
                FosdemApi.downloadSchedule(this)
            }
        }
    }

    override fun onStop() {
        searchMenuItem?.run {
            if (isActionViewExpanded) {
                collapseActionView()
            }
        }

        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        this.searchMenuItem = menu.findItem(R.id.search)?.apply {
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    // Workaround for disappearing menu items bug
                    invalidateOptionsMenu()
                    return true
                }
            })

            // Associate searchable configuration with the SearchView
            val searchManager: SearchManager? = getSystemService()
            (actionView as SearchView).setSearchableInfo(searchManager?.getSearchableInfo(componentName))
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Will close the drawer if the home button is pressed
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        return when (item.itemId) {
            R.id.refresh -> {
                val icon = item.icon
                if (icon is Animatable) {
                    // Hack: reset the icon to make sure the MenuItem will redraw itself properly
                    item.icon = icon
                    icon.start()
                }
                FosdemApi.downloadSchedule(this)
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
                    add(R.id.content, section.createFragment(), section.name)
                }
            }

            currentSection = section
            updateActionBar(section, menuItem)
        }
    }

    override fun createNfcAppData(): NdefRecord? {
        // Delegate to the currently displayed fragment if it provides NFC data
        return (supportFragmentManager.findFragmentById(R.id.content) as? CreateNfcAppDataCallback)?.createNfcAppData()
    }

    companion object {
        const val ACTION_SHORTCUT_BOOKMARKS = "${BuildConfig.APPLICATION_ID}.intent.action.SHORTCUT_BOOKMARKS"
        const val ACTION_SHORTCUT_LIVE = "${BuildConfig.APPLICATION_ID}.intent.action.SHORTCUT_LIVE"

        private const val ERROR_MESSAGE_DISPLAY_DURATION = 5000
        private const val DATABASE_VALIDITY_DURATION = DateUtils.DAY_IN_MILLIS
        private const val AUTO_UPDATE_SNOOZE_DURATION = DateUtils.DAY_IN_MILLIS
        private const val PREF_LATEST_AUTO_UPDATE_ATTEMPT_TIME = "last_download_reminder_time"
        private const val LATEST_UPDATE_DATE_FORMAT = "d MMM yyyy kk:mm:ss"
    }
}