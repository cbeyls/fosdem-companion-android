package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.Track

object GlobalTypeConverters {
    @JvmStatic
    @TypeConverter
    fun fromDay(day: Day): Long = day.index.toLong()

    @JvmStatic
    @TypeConverter
    fun fromTrack(track: Track): Long = track.id

    @JvmStatic
    @TypeConverter
    fun fromPerson(person: Person): Long = person.id

    @JvmStatic
    @TypeConverter
    fun fromEvent(event: Event): Long = event.id
}