package be.digitalia.fosdem.inject

import be.digitalia.fosdem.activities.EventDetailsActivity
import be.digitalia.fosdem.activities.ExternalBookmarksActivity
import be.digitalia.fosdem.activities.HomeActivity
import be.digitalia.fosdem.activities.PersonInfoActivity
import be.digitalia.fosdem.activities.RoomImageDialogActivity
import be.digitalia.fosdem.activities.SearchResultActivity
import be.digitalia.fosdem.activities.SettingsActivity
import be.digitalia.fosdem.activities.TrackScheduleActivity
import be.digitalia.fosdem.activities.TrackScheduleEventActivity

interface AndroidInjectors {
    fun inject(activity: EventDetailsActivity)
    fun inject(activity: ExternalBookmarksActivity)
    fun inject(activity: HomeActivity)
    fun inject(activity: PersonInfoActivity)
    fun inject(activity: RoomImageDialogActivity)
    fun inject(activity: SearchResultActivity)
    fun inject(activity: SettingsActivity)
    fun inject(activity: TrackScheduleActivity)
    fun inject(activity: TrackScheduleEventActivity)
}
