package be.digitalia.fosdem.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Utility class to perform HTTP requests.
 * 
 * @author Christophe Beyls
 */
public class HttpUtils {

	private static final int DEFAULT_TIMEOUT = 10000;

	static {
		// HTTP connection reuse was buggy pre-froyo
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}

		// Bypass hostname verification
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		// Trust all HTTPS certificates
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		} };
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static InputStream get(Context context, String path) throws IOException {
		return get(context, new URL(path), null, null);
	}

	public static InputStream get(Context context, String path, String progressAction, String progressExtra) throws IOException {
		return get(context, new URL(path), progressAction, progressExtra);
	}

	public static InputStream get(final Context context, URL url, final String progressAction, final String progressExtra) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setReadTimeout(DEFAULT_TIMEOUT);
		connection.setConnectTimeout(DEFAULT_TIMEOUT);
		connection.connect();

		if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			connection.disconnect();

			throw new IOException("Server returned response code: " + connection.getResponseCode());
		}

		final int length = connection.getContentLength();
		InputStream is = new BufferedInputStream(connection.getInputStream());
		if ((progressAction == null) || (length == -1)) {
			// No progress support
			return is;
		}

		// Broadcast the progression in percents, with a precision of 1/10 of the total file size
		return new ByteCountInputStream(is, new ByteCountInputStream.ByteCountListener() {

			private LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);

			@Override
			public void onNewCount(int byteCount) {
				// Cap percent to 100
				int percent = (byteCount >= length) ? 100 : byteCount * 100 / length;
				lbm.sendBroadcast(new Intent(progressAction).putExtra(progressExtra, percent));
			}
		}, length / 10);
	}
}
