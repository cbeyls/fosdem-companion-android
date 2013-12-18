package be.digitalia.fosdem.utils;

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
		char[] result = new char[source.length()];
		char c;
		for (int i = 0; i < source.length(); i++) {
			c = source.charAt(i);
			if (c >= '\u00c0' && c <= '\u017f') {
				c = tab00c0.charAt((int) c - '\u00c0');
			}
			result[i] = c;
		}
		return new String(result);
	}

	public static String toSlug(String source) {
		return removeDiacritics(source).replace(" ", "");
	}
}
