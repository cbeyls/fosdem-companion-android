package be.digitalia.fosdem.utils

import okio.Buffer
import okio.ForwardingSource
import okio.Source
import java.io.IOException

/**
 * A Source which counts the total number of bytes read and notifies a listener.
 *
 * @author Christophe Beyls
 */
class ByteCountSource(input: Source,
                      private val listener: ByteCountListener,
                      private val interval: Long) : ForwardingSource(input) {

    interface ByteCountListener {
        fun onNewCount(byteCount: Long)
    }

    private var currentBytes: Long = 0
    private var nextStepBytes: Long = interval

    init {
        require(interval > 0L) { "interval must be at least 1 byte" }
        listener.onNewCount(0L)
    }

    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
        val count = super.read(sink, byteCount)
        if (count != -1L) {
            currentBytes += count
            if (currentBytes < nextStepBytes) {
                return count
            }
            nextStepBytes = currentBytes + interval
        }
        listener.onNewCount(currentBytes)
        return count
    }
}