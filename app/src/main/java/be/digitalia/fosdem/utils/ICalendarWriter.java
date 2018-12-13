package be.digitalia.fosdem.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Simple wrapper to write to iCalendar file format.
 */
public class ICalendarWriter implements Closeable {

	private static final String CRLF = "\r\n";

	private final Writer writer;

	public ICalendarWriter(@NonNull Writer writer) {
		this.writer = writer;
	}

	public void write(@NonNull String key, @Nullable String value) throws IOException {
		if (value != null) {
			writer.write(key);
			writer.write(':');

			// Escape line break sequences
			final int length = value.length();
			int start = 0;
			int end = 0;
			while (end < length) {
				final char c = value.charAt(end);
				if (c == '\r' || c == '\n') {
					writer.write(value, start, end - start);
					writer.write(CRLF);
					writer.write(' ');
					do {
						end++;
					}
					while ((end < length) && (value.charAt(end) == '\r' || value.charAt(end) == '\n'));
					start = end;
				} else {
					end++;
				}
			}
			writer.write(value, start, end - start);

			writer.write(CRLF);
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
