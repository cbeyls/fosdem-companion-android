package be.digitalia.fosdem.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility class to perform HTTP requests.
 *
 * @author Christophe Beyls
 */
public class HttpUtils {

	private static final int DEFAULT_TIMEOUT = 10000;
	private static final int BUFFER_SIZE = 8192;

	public static class HttpResult {
		// Will be null when the local content is up-to-date
		public InputStream inputStream;
		public String lastModified;
	}

	public interface ProgressUpdateListener {
		void onProgressUpdate(int percent);
	}

	public static InputStream get(@NonNull String path) throws IOException {
		return get(new URL(path), null, null).inputStream;
	}

	public static HttpResult get(@NonNull String path, @Nullable String lastModified, @Nullable ProgressUpdateListener listener)
			throws IOException {
		return get(new URL(path), lastModified, listener);
	}

	public static HttpResult get(@NonNull URL url, @Nullable String lastModified, @Nullable final ProgressUpdateListener listener)
			throws IOException {
		HttpResult result = new HttpResult();

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setReadTimeout(DEFAULT_TIMEOUT);
		connection.setConnectTimeout(DEFAULT_TIMEOUT);
		// We handle gzip manually to avoid EOFException bug in many Android versions when server returns HTTP 304
		connection.addRequestProperty("Accept-Encoding", "gzip");
		if (lastModified != null) {
			connection.addRequestProperty("If-Modified-Since", lastModified);
		}
		connection.connect();

		String contentEncoding = connection.getHeaderField("Content-Encoding");
		result.lastModified = connection.getHeaderField("Last-Modified");

		int responseCode = connection.getResponseCode();
		if (responseCode != HttpURLConnection.HTTP_OK) {
			connection.disconnect();

			if ((responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) && (lastModified != null)) {
				// Cached result is still valid; return an empty response
				return result;
			}

			throw new IOException("Server returned response code: " + responseCode);
		}

		final int length = connection.getContentLength();
		result.inputStream = connection.getInputStream();

		if ((listener != null) && (length != -1)) {
			// Broadcast the progression in percents, with a precision of 1/10 of the total file size
			result.inputStream = new ByteCountInputStream(result.inputStream,
					new ByteCountInputStream.ByteCountListener() {
						@Override
						public void onNewCount(int byteCount) {
							// Cap percent to 100
							int percent = (byteCount >= length) ? 100 : byteCount * 100 / length;
							listener.onProgressUpdate(percent);
						}
					}, length / 10);
		}

		if ("gzip".equals(contentEncoding)) {
			result.inputStream = new GZIPInputStream(result.inputStream, BUFFER_SIZE);
		} else {
			result.inputStream = new BufferedInputStream(result.inputStream, BUFFER_SIZE);
		}
		return result;
	}
}
