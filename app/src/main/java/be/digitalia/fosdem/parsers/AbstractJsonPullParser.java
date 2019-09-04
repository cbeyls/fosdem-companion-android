package be.digitalia.fosdem.parsers;

import android.util.JsonReader;

import java.io.InputStreamReader;

import okio.BufferedSource;

public abstract class AbstractJsonPullParser<T> implements Parser<T> {

	@Override
	public T parse(BufferedSource source) throws Exception {
		JsonReader reader = new JsonReader(new InputStreamReader(source.inputStream()));
		return parse(reader);
	}

	protected abstract T parse(JsonReader reader) throws Exception;
}
