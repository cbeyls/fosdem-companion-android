package be.digitalia.fosdem.providers

import android.app.Activity
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.annotation.WorkerThread
import androidx.core.app.ShareCompat
import androidx.core.content.ContentProviderCompat
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.R
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.db.BookmarksDao
import be.digitalia.fosdem.db.ScheduleDao
import be.digitalia.fosdem.ical.ICalendarWriter
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.utils.BackgroundWorkScope
import be.digitalia.fosdem.utils.stripHtml
import be.digitalia.fosdem.utils.toLocalDateTime
import be.digitalia.fosdem.utils.toSlug
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Content Provider generating the current bookmarks list in iCalendar format.
 */
class BookmarksExportProvider : ContentProvider() {

    // Manual dependency injection

    private val scheduleDao: ScheduleDao by lazy {
        EntryPointAccessors.fromApplication(
            ContentProviderCompat.requireContext(this),
            BookmarksExportProviderEntryPoint::class.java
        ).scheduleDao
    }
    private val bookmarksDao: BookmarksDao by lazy {
        EntryPointAccessors.fromApplication(
            ContentProviderCompat.requireContext(this),
            BookmarksExportProviderEntryPoint::class.java
        ).bookmarksDao
    }

    override fun onCreate() = true

    override fun insert(uri: Uri, values: ContentValues?) = throw UnsupportedOperationException()

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = throw UnsupportedOperationException()

    override fun getType(uri: Uri) = TYPE

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val ctx = ContentProviderCompat.requireContext(this)
        val proj = projection ?: COLUMNS
        val cols = arrayOfNulls<String>(proj.size)
        val values = arrayOfNulls<Any>(proj.size)
        var columnCount = 0
        for (col in proj) {
            when (col) {
                OpenableColumns.DISPLAY_NAME -> {
                    cols[columnCount] = OpenableColumns.DISPLAY_NAME
                    val conferenceTitle = runBlocking { scheduleDao.conferenceTitle.first() }
                        ?: ctx.getString(R.string.app_name)
                    values[columnCount++] =
                        ctx.getString(R.string.export_bookmarks_file_name, conferenceTitle)
                }
                OpenableColumns.SIZE -> {
                    cols[columnCount] = OpenableColumns.SIZE
                    // Unknown size, content will be generated on-the-fly
                    values[columnCount++] = 1024L
                }
            }
        }

        val cursor = MatrixCursor(cols.copyOfRange(0, columnCount), 1)
        cursor.addRow(values.copyOfRange(0, columnCount))
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return try {
            val pipe = ParcelFileDescriptor.createPipe()
            val bufferedSink = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).sink().buffer()
            BackgroundWorkScope.launch(Dispatchers.IO) {
                try {
                    writeBookmarks(
                        bufferedSink = bufferedSink,
                        bookmarks = bookmarksDao.getBookmarks(),
                        applicationId = BuildConfig.APPLICATION_ID,
                        applicationVersion = BuildConfig.VERSION_NAME,
                        conferenceId = scheduleDao.getYear().toString(),
                        baseUrl = scheduleDao.baseUrl.first(),
                        dtStamp = LocalDateTime.now(ZoneOffset.UTC).format(UTC_DATE_TIME_FORMAT)
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }
                }
            }
            pipe[0]
        } catch (e: IOException) {
            throw FileNotFoundException("Could not open pipe")
        }
    }

    @WorkerThread
    @Throws(IOException::class)
    private fun writeBookmarks(
        bufferedSink: BufferedSink,
        bookmarks: List<Event>,
        applicationId: String,
        applicationVersion: String,
        conferenceId: String,
        baseUrl: String?,
        dtStamp: String
    ) {
        ICalendarWriter(bufferedSink).use { writer ->
            writer.write("BEGIN", "VCALENDAR")
            writer.write("VERSION", "2.0")
            writer.write("PRODID", "-//$applicationId//NONSGML $applicationVersion//EN")

            for (event in bookmarks) {
                writeEvent(writer, event, applicationId, conferenceId, baseUrl, dtStamp)
            }

            writer.write("END", "VCALENDAR")
        }
    }

    @WorkerThread
    @Throws(IOException::class)
    private fun writeEvent(
        writer: ICalendarWriter,
        event: Event,
        applicationId: String,
        conferenceId: String,
        baseUrl: String?,
        dtStamp: String
    ) = with(writer) {
        write("BEGIN", "VEVENT")

        write("UID", "${event.id}@$conferenceId@$applicationId")
        write("DTSTAMP", dtStamp)
        event.startTime?.let { write("DTSTART", it.toLocalDateTime(ZoneOffset.UTC).format(UTC_DATE_TIME_FORMAT)) }
        event.endTime?.let { write("DTEND", it.toLocalDateTime(ZoneOffset.UTC).format(UTC_DATE_TIME_FORMAT)) }
        write("SUMMARY", event.title)
        var description = event.abstractText
        if (description.isNullOrEmpty()) {
            description = event.description
        }
        if (!description.isNullOrEmpty()) {
            write("DESCRIPTION", description.stripHtml())
            write("X-ALT-DESC", description)
        }
        write("CLASS", "PUBLIC")
        write("CATEGORIES", event.track.name)
        write("URL", event.url)
        write("LOCATION", event.roomName)

        if (event.personsSummary != null && baseUrl != null) {
            for (name in event.personsSummary.split(", ")) {
                val key = "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"$name\""
                val url = FosdemUrls.getPerson(baseUrl, name.toSlug())
                write(key, url)
            }
        }

        write("END", "VEVENT")
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BookmarksExportProviderEntryPoint {
        val scheduleDao: ScheduleDao
        val bookmarksDao: BookmarksDao
    }

    companion object {
        const val TYPE = "text/calendar"
        private val URI = Uri.Builder()
                .scheme("content")
                .authority("${BuildConfig.APPLICATION_ID}.bookmarks")
                .appendPath("bookmarks.ics")
                .build()
        private val COLUMNS = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        private val UTC_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)

        fun getIntent(activity: Activity): Intent {
            // Supports granting read permission for the attached shared file
            return ShareCompat.IntentBuilder(activity)
                    .setStream(URI)
                    .setType(TYPE)
                    .intent
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}