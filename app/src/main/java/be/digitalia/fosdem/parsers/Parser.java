package be.digitalia.fosdem.parsers;

import java.io.InputStream;

public interface Parser<T> {
	T parse(InputStream is) throws Exception;
}
