package be.digitalia.fosdem.utils;

import androidx.annotation.NonNull;

import java.io.IOException;

import okio.Buffer;
import okio.ForwardingSource;
import okio.Source;

/**
 * A Source which counts the total number of bytes read and notifies a listener.
 *
 * @author Christophe Beyls
 */
public class ByteCountSource extends ForwardingSource {

	public interface ByteCountListener {
		void onNewCount(long byteCount);
	}

	private final ByteCountListener listener;
	private final long interval;
	private long currentBytes = 0;
	private long nextStepBytes;

	public ByteCountSource(@NonNull Source input, @NonNull ByteCountListener listener, long interval) {
		super(input);
		if (interval <= 0) {
			throw new IllegalArgumentException("interval must be at least 1 byte");
		}
		this.listener = listener;
		this.interval = interval;
		nextStepBytes = interval;
		listener.onNewCount(0L);
	}

	@Override
	public long read(Buffer sink, long byteCount) throws IOException {
		final long count = super.read(sink, byteCount);

		if (count != -1L) {
			currentBytes += count;
			if (currentBytes < nextStepBytes) {
				return count;
			}
			nextStepBytes = currentBytes + interval;
		}
		listener.onNewCount(currentBytes);

		return count;
	}
}
