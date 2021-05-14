package be.digitalia.fosdem.ical

import be.digitalia.fosdem.ical.internal.CRLF
import okio.Buffer
import okio.BufferedSource
import java.io.Closeable
import java.io.IOException

/**
 * Optimized streaming ICalendar reader.
 */
class ICalendarReader(private val source: BufferedSource) : Closeable {
    private var state = STATE_BEGIN_KEY

    class Options private constructor(internal val suffixedKey: okio.Options) {
        companion object {
            fun of(vararg keys: String): Options {
                val buffer = Buffer()
                val result = Array(keys.size) {
                    buffer.writeUtf8(keys[it])
                    buffer.writeByte(':'.code)
                    buffer.readByteString()
                }
                return Options(okio.Options.of(*result))
            }
        }
    }

    fun hasNext(): Boolean = !source.exhausted()

    fun nextKey(): String {
        check(state == STATE_BEGIN_KEY)
        val endPosition = source.indexOf(':'.code.toByte())
        endPosition >= 0L || throw IOException("Invalid key")
        val result = source.readUtf8(endPosition)
        source.skip(1L)
        state = STATE_BEGIN_VALUE
        return result
    }

    fun skipKey() {
        check(state == STATE_BEGIN_KEY)
        val endPosition = source.indexOf(':'.code.toByte())
        endPosition >= 0L || throw IOException("Invalid key")
        source.skip(endPosition + 1L)
        state = STATE_BEGIN_VALUE
    }

    fun selectKey(options: Options): Int {
        check(state == STATE_BEGIN_KEY)
        val result = source.select(options.suffixedKey)
        if (result >= 0) {
            state = STATE_BEGIN_VALUE
        }
        return result
    }

    private inline fun BufferedSource.unfoldLines(lineLambda: BufferedSource.(endPosition: Long) -> Unit) {
        while (true) {
            val endPosition = indexOf(CRLF)
            if (endPosition < 0L) {
                // buffer now contains the rest of the file until the end
                lineLambda(buffer.size)
                break
            }
            lineLambda(endPosition)
            skip(2L)
            if (!request(1L) || buffer[0L] != ' '.code.toByte()) {
                break
            }
            skip(1L)
        }
    }

    fun nextValue(): String {
        check(state == STATE_BEGIN_VALUE)
        val resultBuffer = Buffer()
        source.unfoldLines { read(resultBuffer, it) }
        state = STATE_BEGIN_KEY
        return resultBuffer.readUtf8()
    }

    fun skipValue() {
        check(state == STATE_BEGIN_VALUE)
        source.unfoldLines { skip(it) }
        state = STATE_BEGIN_KEY
    }

    @Throws(IOException::class)
    override fun close() {
        source.close()
    }

    companion object {
        private const val STATE_BEGIN_KEY = 0
        private const val STATE_BEGIN_VALUE = 1
    }
}