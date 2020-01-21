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
import androidx.core.app.ShareCompat
import be.digitalia.fosdem.BuildConfig
import be.digitalia.fosdem.R
import be.digitalia.fosdem.api.FosdemUrls.getPerson
import be.digitalia.fosdem.db.AppDatabase
import be.digitalia.fosdem.model.Event
import be.digitalia.fosdem.utils.DateUtils
import be.digitalia.fosdem.utils.ICalendarWriter
import be.digitalia.fosdem.utils.stripHtml
import be.digitalia.fosdem.utils.toSlug
import okio.buffer
import okio.sink
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Content Provider generating the current bookmarks list in iCalendar format.
 */
class BookmarksExportProvider : ContentProvider() {

    override fun onCreate() = true

    override fun insert(uri: Uri, values: ContentValues?) = throw UnsupportedOperationException()

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = throw UnsupportedOperationException()

    override fun getType(uri: Uri) = TYPE

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val ctx = context!!
        val proj = projection ?: COLUMNS
        val cols = arrayOfNulls<String>(proj.size)
        val values = arrayOfNulls<Any>(proj.size)
        var columnCount = 0
        for (col in proj) {
            when (col) {
                OpenableColumns.DISPLAY_NAME -> {
                    cols[columnCount] = OpenableColumns.DISPLAY_NAME
                    values[columnCount++] = ctx.getString(R.string.export_bookmarks_file_name, AppDatabase.getInstance(ctx).scheduleDao.year)
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
            DownloadThread(
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]),
                    AppDatabase.getInstance(context!!)
            ).start()
            pipe[0]
        } catch (e: IOException) {
            throw FileNotFoundException("Could not open pipe")
        }
    }

    private class DownloadThread(private val outputStream: OutputStream, private val appDatabase: AppDatabase) : Thread() {
        private val calendar = Calendar.getInstance(DateUtils.belgiumTimeZone, Locale.US)
        // Format all times in GMT
        private val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+0")
        }
        private val dtStamp = dateFormat.format(System.currentTimeMillis())

        override fun run() {
            try {
                ICalendarWriter(outputStream.sink().buffer()).use { writer ->
                    val bookmarks = appDatabase.bookmarksDao.bookmarks
                    writer.write("BEGIN", "VCALENDAR")
                    writer.write("VERSION", "2.0")
                    writer.write("PRODID", "-//${BuildConfig.APPLICATION_ID}//NONSGML ${BuildConfig.VERSION_NAME}//EN")

                    for (event in bookmarks) {
                        writeEvent(writer, event)
                    }

                    writer.write("END", "VCALENDAR")
                }
            } catch (ignore: Exception) {
            }
        }

        @Throws(IOException::class)
        private fun writeEvent(writer: ICalendarWriter, event: Event) {
            with(writer) {
                write("BEGIN", "VEVENT")

                val year = DateUtils.getYear(event.day.date.time, calendar)
                write("UID", "${event.id}@${year}@${BuildConfig.APPLICATION_ID}")
                write("DTSTAMP", dtStamp)
                event.startTime?.let { write("DTSTART", dateFormat.format(it)) }
                event.endTime?.let { write("DTEND", dateFormat.format(it)) }
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

                for (name in event.personsSummary.split(", ")) {
                    val key = "ATTENDEE;ROLE=REQ-PARTICIPANT;CUTYPE=INDIVIDUAL;CN=\"$name\""
                    val url = getPerson(name.toSlug(), year)
                    write(key, url)
                }

                write("END", "VEVENT")
            }
        }
    }

    companion object {
        private val URI = Uri.Builder()
                .scheme("content")
                .authority("${BuildConfig.APPLICATION_ID}.bookmarks")
                .appendPath("bookmarks.ics")
                .build()
        private const val TYPE = "text/calendar"
        private val COLUMNS = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)

        fun getIntent(activity: Activity?): Intent {
            // Supports granting read permission for the attached shared file
            return ShareCompat.IntentBuilder.from(activity!!)
                    .setStream(URI)
                    .setType(TYPE)
                    .intent
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}