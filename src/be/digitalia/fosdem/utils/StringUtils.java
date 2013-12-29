package be.digitalia.fosdem.utils;

import java.util.Locale;

/**
 * Various methods to transform strings
 * 
 * @author Christophe Beyls
 */
public class StringUtils {
	/**
	 * Mirror of the unicode table from 00c0 to 017f without diacritics.
	 */
	private static final String tab00c0 = "AAAAAAACEEEEIIII" + "DNOOOOO\u00d7\u00d8UUUUYI\u00df" + "aaaaaaaceeeeiiii" + "\u00f0nooooo\u00f7\u00f8uuuuy\u00fey"
			+ "AaAaAaCcCcCcCcDd" + "DdEeEeEeEeEeGgGg" + "GgGgHhHhIiIiIiIi" + "IiJjJjKkkLlLlLlL" + "lLlNnNnNnnNnOoOo" + "OoOoRrRrRrSsSsSs" + "SsTtTtTtUuUuUuUu"
			+ "UuUuWwYyYZzZzZzF";

	/**
	 * Returns string without diacritics - 7 bit approximation.
	 * 
	 * @param source
	 *            string to convert
	 * @return corresponding string without diacritics
	 */
	public static String removeDiacritics(String source) {
		final int length = source.length();
		char[] result = new char[length];
		char c;
		for (int i = 0; i < length; i++) {
			c = source.charAt(i);
			if (c >= '\u00c0' && c <= '\u017f') {
				c = tab00c0.charAt((int) c - '\u00c0');
			}
			result[i] = c;
		}
		return new String(result);
	}

	/**
	 * Replaces all groups of non-alphanumeric chars in source with a single replacement char.
	 */
	private static String replaceNonAlphaGroups(String source, char replacement) {
		final int length = source.length();
		char[] result = new char[length];
		char c;
		boolean replaced = false;
		int size = 0;
		for (int i = 0; i < length; i++) {
			c = source.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				result[size++] = c;
				replaced = false;
			} else {
				if (!replaced) {
					result[size++] = replacement;
					replaced = true;
				}
			}
		}
		return new String(result, 0, size);
	}

	public static String toSlug(String source) {
		return replaceNonAlphaGroups(removeDiacritics(source), '_').toLowerCase(Locale.US);
	}

	public static CharSequence trimEnd(CharSequence source) {
		final int length = source.length();
		int pos = length - 1;
		while (Character.isWhitespace(source.charAt(pos))) {
			pos--;
		}
		pos++;
		if (pos == length) {
			// No whitespace found at the end
			return source;
		}
		return source.subSequence(0, pos);
	}
}
