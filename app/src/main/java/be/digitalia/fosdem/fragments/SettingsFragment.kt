package be.digitalia.fosdem.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.R
import be.digitalia.fosdem.alarms.AppAlarmManager
import be.digitalia.fosdem.settings.PreferenceKeys
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var alarmManager: AppAlarmManager

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result: Boolean ->
            if (result && !requestScheduleExactAlarmIfNeeded()) {
                checkNotificationsEnabledPreference()
            }
        }

    private val requestExactAlarmsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            // The activity result does not always reflect if scheduling was allowed or not
            if (result.resultCode == Activity.RESULT_OK || alarmManager.canScheduleExactAlarms) {
                checkNotificationsEnabledPreference()
            }
        }

    @SuppressLint("InlinedApi")
    private fun requestNotificationsPermissionIfNeeded(): Boolean {
        val isRequestNeeded = !alarmManager.hasNotificationsPermission
        if (isRequestNeeded) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        return isRequestNeeded
    }

    @SuppressLint("InlinedApi")
    private fun requestScheduleExactAlarmIfNeeded(): Boolean {
        val isRequestNeeded = !alarmManager.canScheduleExactAlarms
        if (isRequestNeeded) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData("package:${BuildConfig.APPLICATION_ID}".toUri())
            requestExactAlarmsLauncher.launch(intent)
        }
        return isRequestNeeded
    }

    private fun checkNotificationsEnabledPreference() {
        val preference =
            findPreference<Preference>(PreferenceKeys.NOTIFICATIONS_ENABLED) as? TwoStatePreference
        preference?.isChecked = true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        setupNotifications()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationsChannel()
        }
        setupAboutDialog()
        populateVersion()
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState).also { recyclerView ->
        // Handle WindowInsets
        recyclerView.clipToPadding = false
        val initialPaddingLeft = recyclerView.paddingLeft
        val initialPaddingRight = recyclerView.paddingRight
        val initialPaddingBottom = recyclerView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val padding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = initialPaddingLeft + padding.left,
                right = initialPaddingRight + padding.right,
                bottom = initialPaddingBottom + padding.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupNotifications() {
        findPreference<Preference>(PreferenceKeys.NOTIFICATIONS_ENABLED)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (requestNotificationsPermissionIfNeeded() || requestScheduleExactAlarmIfNeeded()) {
                        return@OnPreferenceChangeListener false
                    }
                }
                true
            }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupNotificationsChannel() {
        findPreference<Preference>(PreferenceKeys.NOTIFICATIONS_CHANNEL)?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                AppAlarmManager.startChannelNotificationSettingsActivity(requireContext())
                true
            }
    }

    private fun setupAboutDialog() {
        findPreference<Preference>(PreferenceKeys.ABOUT)?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                AboutDialogFragment().show(parentFragmentManager, "about")
                true
            }
    }

    class AboutDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(resources.getText(R.string.about_text))
                .setPositiveButton(android.R.string.ok, null)
                .create()
        }

        override fun onStart() {
            super.onStart()
            // Make links clickable; must be called after the dialog is shown
            requireDialog().findViewById<TextView>(android.R.id.message).movementMethod =
                LinkMovementMethodCompat.getInstance()
        }
    }

    private fun populateVersion() {
        findPreference<Preference>(PreferenceKeys.VERSION)?.summary = BuildConfig.VERSION_NAME
    }
}