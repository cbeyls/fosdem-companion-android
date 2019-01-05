package be.digitalia.fosdem.utils;

import android.content.res.Resources;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;

import org.xml.sax.XMLReader;

import java.util.Iterator;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.collection.CircularIntArray;

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

	private static final String ROOM_DRAWABLE_PREFIX = "room_";

	/**
	 * Returns string without diacritics - 7 bit approximation.
	 *
	 * @param source string to convert
	 * @return corresponding string without diacritics
	 */
	public static String removeDiacritics(@NonNull String source) {
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

	public static String remove(String str, final char remove) {
		if (TextUtils.isEmpty(str) || str.indexOf(remove) == -1) {
			return str;
		}
		final char[] chars = str.toCharArray();
		int pos = 0;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] != remove) {
				chars[pos++] = chars[i];
			}
		}
		return new String(chars, 0, pos);
	}

	/**
	 * Replaces all groups of removable chars in source with a single replacement char.
	 */
	private static String replaceNonAlphaGroups(String source, char replacement) {
		final int length = source.length();
		char[] result = new char[length];
		char c;
		boolean replaced = false;
		int size = 0;
		for (int i = 0; i < length; i++) {
			c = source.charAt(i);
			if (isRemovableChar(c)) {
				// Skip quote
				if ((c != '’') && !replaced) {
					result[size++] = replacement;
					replaced = true;
				}
			} else {
				result[size++] = c;
				replaced = false;
			}
		}
		return new String(result, 0, size);
	}

	/**
	 * Removes all removable chars at the beginning and end of source.
	 */
	private static String trimNonAlpha(String source) {
		int st = 0;
		int len = source.length();

		while ((st < len) && isRemovableChar(source.charAt(st))) {
			st++;
		}
		while ((st < len) && isRemovableChar(source.charAt(len - 1))) {
			len--;
		}
		return ((st > 0) || (len < source.length())) ? source.substring(st, len) : source;
	}

	private static boolean isRemovableChar(char c) {
		return !Character.isLetterOrDigit(c) && c != '_' && c != '@';
	}

	/**
	 * Transforms a name to a slug identifier to be used in a FOSDEM URL.
	 */
	public static String toSlug(@NonNull String source) {
		source = remove(source, '.');
		source = removeDiacritics(source);
		source = source.replace("ß", "ss");
		source = trimNonAlpha(source);
		source = replaceNonAlphaGroups(source, '_');
		source = source.toLowerCase(Locale.US);
		return source;
	}

	@SuppressWarnings("deprecation")
	public static String stripHtml(@NonNull String html) {
		return trimEnd(Html.fromHtml(html)).toString();
	}

	@SuppressWarnings("deprecation")
	public static CharSequence parseHtml(@NonNull String html, Resources res) {
		return trimEnd(Html.fromHtml(html, null, new ListsTagHandler(res)));
	}

	public static CharSequence trimEnd(@NonNull CharSequence source) {
		int pos = source.length() - 1;
		while ((pos >= 0) && Character.isWhitespace(source.charAt(pos))) {
			pos--;
		}
		pos++;
		return (pos < source.length()) ? source.subSequence(0, pos) : source;
	}

	/**
	 * Converts a room name to a local drawable resource name, by stripping non-alpha chars and converting to lower case. Any letter following a digit will be
	 * ignored, along with the rest of the string.
	 */
	public static String roomNameToResourceName(@NonNull String roomName) {
		StringBuilder builder = new StringBuilder(ROOM_DRAWABLE_PREFIX.length() + roomName.length());
		builder.append(ROOM_DRAWABLE_PREFIX);
		int size = roomName.length();
		boolean lastDigit = false;
		for (int i = 0; i < size; ++i) {
			char c = roomName.charAt(i);
			if (Character.isLetter(c)) {
				if (lastDigit) {
					break;
				}
				builder.append(Character.toLowerCase(c));
			} else if (Character.isDigit(c)) {
				builder.append(c);
				lastDigit = true;
			}
		}
		return builder.toString();
	}

	static class ListsTagHandler implements Html.TagHandler {

		private static final float LEADING_MARGIN_DIPS = 2f;
		private static final float BULLET_GAP_WIDTH_DIPS = 8f;

		private final CircularIntArray liStarts = new CircularIntArray(4);
		private final int leadingMargin;
		private final int bulletGapWidth;

		public ListsTagHandler(Resources res) {
			final float density = res.getDisplayMetrics().density;
			leadingMargin = (int) (density * LEADING_MARGIN_DIPS + 0.5f);
			bulletGapWidth = (int) (density * BULLET_GAP_WIDTH_DIPS + 0.5f);
		}

		/**
		 * @return final output length
		 */
		private static int ensureParagraphBoundary(Editable output) {
			int length = output.length();
			if ((length != 0) && output.charAt(length - 1) != '\n') {
				output.insert(length, "\n");
				length++;
			}
			return length;
		}

		private static void trimStart(Editable output, final int start) {
			int end = start;
			final int length = output.length();
			while ((end < length) && Character.isWhitespace(output.charAt(end))) {
				end++;
			}
			if (start < end) {
				output.delete(start, end);
			}
		}

		@Override
		public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
			switch (tag) {
				case "pre":
				case "PRE":
					ensureParagraphBoundary(output);
					break;
				// Unfortunately the following code will be ignored in API 24+ and the native rendering is inferior
				case "li":
				case "LI":
					if (opening) {
						liStarts.addLast(ensureParagraphBoundary(output));
					} else if (!liStarts.isEmpty()) {
						int start = liStarts.popLast();
						trimStart(output, start);
						int end = ensureParagraphBoundary(output);
						// Add leading margin to ensure the bullet is not cut off
						output.setSpan(new LeadingMarginSpan.Standard(leadingMargin), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
						output.setSpan(new BulletSpan(bulletGapWidth), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
					}
					break;
			}
		}
	}

	/**
	 * A version of Android's SimpleStringSplitter using a String as delimiter.
	 */
	public static class SimpleStringSplitter implements TextUtils.StringSplitter, Iterator<String> {
		private final String mDelimiter;
		private String mString;
		private int mPosition;
		private int mLength;

		/**
		 * Initializes the splitter. setString may be called later.
		 *
		 * @param delimiter the delimiter on which to split
		 */
		public SimpleStringSplitter(String delimiter) {
			mDelimiter = delimiter;
		}

		/**
		 * Sets the string to split
		 *
		 * @param string the string to split
		 */
		public void setString(String string) {
			mString = string;
			mPosition = 0;
			mLength = mString.length();
		}

		@NonNull
		public Iterator<String> iterator() {
			return this;
		}

		public boolean hasNext() {
			return mPosition < mLength;
		}

		public String next() {
			int end = mString.indexOf(mDelimiter, mPosition);
			if (end == -1) {
				end = mLength;
			}
			String nextString = mString.substring(mPosition, end);
			mPosition = end + mDelimiter.length(); // Skip the delimiter.
			return nextString;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
