package be.digitalia.fosdem.utils.network;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import be.digitalia.fosdem.utils.ByteCountSource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

/**
 * Utility class to perform HTTP requests.
 *
 * @author Christophe Beyls
 */
public class HttpUtils {

	private static final long DEFAULT_CONNECT_TIMEOUT = 10L;
	private static final long DEFAULT_READ_TIMEOUT = 10L;

	private static OkHttpClient sClient = enableTls12(new OkHttpClient.Builder())
			.connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
			.readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
			.build();

	private HttpUtils() {
	}

	public static class Response {
		// Will be null when the local content is up-to-date
		@Nullable
		public BufferedSource source;
		@Nullable
		public String lastModified;
	}

	public interface ProgressUpdateListener {
		void onProgressUpdate(int percent);
	}

	public static BufferedSource get(@NonNull String path) throws IOException {
		return get(new URL(path), null, null).source;
	}

	public static Response get(@NonNull String path, @Nullable String lastModified, @Nullable ProgressUpdateListener listener)
			throws IOException {
		return get(new URL(path), lastModified, listener);
	}

	public static Response get(@NonNull URL url, @Nullable String lastModified, @Nullable final ProgressUpdateListener listener)
			throws IOException {
		Request.Builder requestBuilder = new Request.Builder();

		if (lastModified != null) {
			requestBuilder.header("If-Modified-Since", lastModified);
		}

		Request request = requestBuilder
				.url(url)
				.build();

		final Response response = new Response();
		final okhttp3.Response okhttpResponse = sClient.newCall(request).execute();
		final ResponseBody body = okhttpResponse.body();
		if (!okhttpResponse.isSuccessful() || (body == null)) {
			if ((okhttpResponse.code() == HttpURLConnection.HTTP_NOT_MODIFIED) && (lastModified != null)) {
				// Cached result is still valid; return an empty response
				return response;
			}

			if (body != null) {
				body.close();
			}
			throw new IOException("Server returned response code: " + okhttpResponse.code());
		}

		response.lastModified = okhttpResponse.header("Last-Modified");

		final long length = body.contentLength();
		if ((listener != null) && (length != -1L)) {
			// Broadcast the progression in percents, with a precision of 1/10 of the total file size
			response.source = Okio.buffer(new ByteCountSource(body.source(),
					byteCount -> {
						// Cap percent to 100
						int percent = (byteCount >= length) ? 100 : (int) (byteCount * 100L / length);
						listener.onProgressUpdate(percent);
					}, length / 10L));
		} else {
			response.source = body.source();
		}

		return response;
	}

	private static OkHttpClient.Builder enableTls12(OkHttpClient.Builder builder) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
			try {
				final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init((KeyStore) null);
				final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
				if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
					throw new IllegalStateException("Unexpected default trust managers: " + Arrays.toString(trustManagers));
				}
				final X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

				final SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, new TrustManager[]{trustManager}, null);

				builder.sslSocketFactory(new Tls12SocketFactory(sslContext.getSocketFactory()), trustManager);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return builder;
	}
}
