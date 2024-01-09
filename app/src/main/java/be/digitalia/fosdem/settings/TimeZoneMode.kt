package be.digitalia.fosdem.settings

import java.time.ZoneId

sealed interface TimeZoneMode {
    val override: ZoneId?

    /**
     * Use the default time zone provided by schedule data.
     */
    data object Default : TimeZoneMode {
        override val override: ZoneId?
            get() = null
    }

    /**
     * Use the device time zone.
     */
    data class Device(override val override: ZoneId) : TimeZoneMode
}