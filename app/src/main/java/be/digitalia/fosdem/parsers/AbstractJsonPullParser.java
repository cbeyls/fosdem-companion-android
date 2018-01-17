package be.digitalia.fosdem.parsers;

import android.util.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class AbstractJsonPullParser<T> implements Parser<T> {

	@Override
	public T parse(InputStream source) throws Exception {
		JsonReader reader = new JsonReader(new InputStreamReader(source));
		return parse(reader);
	}

	protected abstract T parse(JsonReader reader) throws Exception;
}
