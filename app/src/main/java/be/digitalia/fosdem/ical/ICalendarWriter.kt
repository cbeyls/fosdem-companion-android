package be.digitalia.fosdem.ical

import okio.BufferedSink
import okio.ByteString.Companion.encodeUtf8
import java.io.Closeable
import java.io.IOException

/**
 * Simple wrapper to write to iCalendar file format.
 */
class ICalendarWriter(private val sink: BufferedSink) : Closeable {

    @Throws(IOException::class)
    fun write(key: String, value: String?) {
        if (value != null) {
            with(sink) {
                writeUtf8(key)
                writeByte(':'.toInt())

                // Escape line break sequences
                val length = value.length
                var start = 0
                var end = 0
                while (end < length) {
                    val c = value[end]
                    if (c == '\r' || c == '\n') {
                        writeUtf8(value, start, end)
                        write(CRLF)
                        writeByte(' '.toInt())
                        do {
                            end++
                        } while (end < length && (value[end] == '\r' || value[end] == '\n'))
                        start = end
                    } else {
                        end++
                    }
                }
                writeUtf8(value, start, length)

                write(CRLF)
            }
        }
    }

    @Throws(IOException::class)
    override fun close() {
        sink.close()
    }

    companion object {
        private val CRLF = "\r\n".encodeUtf8()
    }

}