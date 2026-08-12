package be.digitalia.fosdem.settings

enum class TimeZoneMode {
    /**
     * Use the default time zone provided by schedule data.
     */
    DEFAULT,

    /**
     * Use the device time zone.
     */
    DEVICE
}
