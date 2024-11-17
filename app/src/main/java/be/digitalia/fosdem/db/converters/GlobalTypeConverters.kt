package be.digitalia.fosdem.db.converters

import androidx.room.TypeConverter
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.Track

object GlobalTypeConverters {
    @TypeConverter
    fun fromDay(day: Day): Long = day.index.toLong()

    @TypeConverter
    fun fromTrack(track: Track): Long = track.id

    @TypeConverter
    fun fromPerson(person: Person): Long = person.id

    @TypeConverter
    fun fromEvent(event: Event): Long = event.id
}