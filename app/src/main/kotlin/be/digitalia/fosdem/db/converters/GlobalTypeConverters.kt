package be.digitalia.fosdem.db.converters

import androidx.room3.ColumnTypeConverter
import be.digitalia.fosdem.model.Day
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.model.Person
import be.digitalia.fosdem.model.Track

object GlobalTypeConverters {
    @ColumnTypeConverter
    fun fromDay(day: Day): Long = day.index.toLong()

    @ColumnTypeConverter
    fun fromTrack(track: Track): Long = track.id

    @ColumnTypeConverter
    fun fromPerson(person: Person): Long = person.id

    @ColumnTypeConverter
    fun fromEvent(event: Event): Long = event.id
}