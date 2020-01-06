package be.digitalia.fosdem.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;

import okio.BufferedSink;

/**
 * Simple wrapper to write to iCalendar file format.
 */
public class ICalendarWriter implements Closeable {

	private static final String CRLF = "\r\n";

	private final BufferedSink sink;

	public ICalendarWriter(@NonNull BufferedSink sink) {
		this.sink = sink;
	}

	public void write(@NonNull String key, @Nullable String value) throws IOException {
		if (value != null) {
			sink.writeUtf8(key);
			sink.writeUtf8CodePoint(':');

			// Escape line break sequences
			final int length = value.length();
			int start = 0;
			int end = 0;
			while (end < length) {
				final char c = value.charAt(end);
				if (c == '\r' || c == '\n') {
					sink.writeUtf8(value, start, end);
					sink.writeUtf8(CRLF);
					sink.writeUtf8CodePoint(' ');
					do {
						end++;
					}
					while ((end < length) && (value.charAt(end) == '\r' || value.charAt(end) == '\n'));
					start = end;
				} else {
					end++;
				}
			}
			sink.writeUtf8(value, start, length);

			sink.writeUtf8(CRLF);
		}
	}

	@Override
	public void close() throws IOException {
		sink.close();
	}
}
