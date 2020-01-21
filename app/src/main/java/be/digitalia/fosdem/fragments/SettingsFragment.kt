package be.digitalia.fosdem.fragments

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.R
import be.digitalia.fosdem.services.AlarmIntentService
import be.digitalia.fosdem.utils.PreferenceKeys
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationsChannel()
        }
        setupAboutDialog()
        populateVersion()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupNotificationsChannel() {
        findPreference<Preference>(PreferenceKeys.NOTIFICATIONS_CHANNEL)?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AlarmIntentService.startChannelNotificationSettingsActivity(requireContext())
            true
        }
    }

    private fun setupAboutDialog() {
        findPreference<Preference>(PreferenceKeys.ABOUT)?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AboutDialogFragment().show(requireFragmentManager(), "about")
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
            requireDialog().findViewById<TextView>(android.R.id.message).movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun populateVersion() {
        findPreference<Preference>(PreferenceKeys.VERSION)?.summary = BuildConfig.VERSION_NAME
    }
}