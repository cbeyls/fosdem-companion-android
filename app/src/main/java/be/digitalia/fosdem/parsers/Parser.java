package be.digitalia.fosdem.parsers;

import okio.BufferedSource;

public interface Parser<T> {
	T parse(BufferedSource source) throws Exception;
}
