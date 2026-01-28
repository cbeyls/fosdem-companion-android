package be.digitalia.fosdem.fragments

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.settings.UserSettingsProvider
import be.digitalia.fosdem.utils.AppTimeSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class DebugTimeTravelDialogFragment : DialogFragment() {

    @Inject
    lateinit var scheduleDao: ScheduleDao

    @Inject
    lateinit var userSettingsProvider: UserSettingsProvider

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, 0)
        }

        val spinner = Spinner(context)
        layout.addView(spinner)

        val timePicker = TimePicker(context).apply {
            setIs24HourView(true)
            hour = 10
            minute = 0
        }
        layout.addView(timePicker)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Time Travel")
            .setView(layout)
            .setPositiveButton("Set", null) // null to prevent auto-dismiss
            .setNeutralButton("Reset") { _, _ ->
                AppTimeSource.offset = null
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        // Load days into spinner and set up button listener after dialog is shown
        dialog.setOnShowListener {
            lifecycleScope.launch {
                val days = scheduleDao.days.first()
                if (days.isEmpty()) {
                    dismiss()
                    return@launch
                }
                val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, days.map { it.name })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    lifecycleScope.launch {
                        val selectedDay = days[spinner.selectedItemPosition]
                        val selectedTime = LocalTime.of(timePicker.hour, timePicker.minute)
                        // Use the same timezone as the calendar view:
                        // - If user selected device timezone, use that
                        // - Otherwise fall back to conference timezone (Brussels)
                        val timeZoneMode = userSettingsProvider.timeZoneMode.first()
                        val zone = timeZoneMode.override ?: ZoneId.of("Europe/Brussels")
                        val desiredInstant = selectedDay.date
                            .atTime(selectedTime)
                            .atZone(zone)
                            .toInstant()
                        AppTimeSource.offset = Duration.between(Instant.now(), desiredInstant)
                        dismiss()
                    }
                }
            }
        }

        return dialog
    }
}
