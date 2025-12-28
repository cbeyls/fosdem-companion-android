package be.digitalia.fosdem.utils

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.Html.ImageGetter
import android.text.Html.TagHandler
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import androidx.collection.CircularIntArray
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.core.text.set
import org.xml.sax.XMLReader
import java.util.Locale

/**
 * Various methods to transform strings
 *
 * @author Christophe Beyls
 */

/**
 * Mirror of the unicode table from 00c0 to 017f without diacritics.
 */
private const val tab00c0 = "AAAAAAACEEEEIIII" + "DNOOOOO\u00d7\u00d8UUUUYI\u00df" +
        "aaaaaaaceeeeiiii" + "\u00f0nooooo\u00f7\u00f8uuuuy\u00fey" +
        "AaAaAaCcCcCcCcDd" + "DdEeEeEeEeEeGgGg" +
        "GgGgHhHhIiIiIiIi" + "IiJjJjKkkLlLlLlL" +
        "lLlNnNnNnnNnOoOo" + "OoOoRrRrRrSsSsSs" +
        "SsTtTtTtUuUuUuUu" + "UuUuWwYyYZzZzZzF"
private const val ROOM_DRAWABLE_PREFIX = "room_"

/**
 * Returns string without diacritics - 7 bit approximation.
 *
 * @return corresponding string without diacritics
 */
fun String.removeDiacritics(): String {
    val result = CharArray(length) { i ->
        var c = this[i]
        if (c in '\u00c0'..'\u017f') {
            c = tab00c0[c.code - '\u00c0'.code]
        }
        c
    }
    return result.concatToString()
}

fun String.remove(remove: Char): String {
    return if (remove !in this) {
        this
    } else {
        filterNot { it == remove }
    }
}

/**
 * Replaces all groups of removable chars in source with a single replacement char.
 */
private fun String.replaceNonAlphaGroups(replacement: Char): String {
    val result = CharArray(length)
    var replaced = false
    var size = 0
    for (c in this) {
        if (c.isRemovable) {
            // Skip quote
            if (c != '’' && !replaced) {
                result[size++] = replacement
                replaced = true
            }
        } else {
            result[size++] = c
            replaced = false
        }
    }
    return result.concatToString(0, size)
}

/**
 * Removes all removable chars at the beginning and end of source.
 */
fun String.trimNonAlpha(): String {
    return trim { it.isRemovable }
}

private val Char.isRemovable: Boolean
    get() {
        return !isLetterOrDigit() && this != '_' && this != '-'
    }

/**
 * Transforms a name to a slug identifier to be used in a FOSDEM URL.
 */
fun String.toSlug(): String {
    return remove('\'')
            .removeDiacritics()
            .replace("ß", "ss")
            .replace('ø', 'o')
            .replace('ð', 'd')
            .trimNonAlpha()
            .replaceNonAlphaGroups('_')
            .lowercase(Locale.ROOT)
}

/**
 * Generates a slug from a FOSDEM room name.
 */
fun String.toRoomSlug(): String {
    return remove('.')
        .removeDiacritics()
        .trimNonAlpha()
        .replaceNonAlphaGroups('_')
        .lowercase(Locale.ROOT)
}

fun String.stripHtml(): String {
    return parseAsHtml(flags = HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM).trimEnd().toString()
}

fun String.parseHtml(res: Resources): CharSequence {
    return parseAsHtml(
        flags = HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM,
        imageGetter = EmptyImageGetter,
        tagHandler = ListsTagHandler(res)
    ).trimEnd()
}

/**
 * Converts a room name to a local drawable resource name, by stripping non-alpha chars and converting to lower case. Any letter following a digit will be
 * ignored, along with the rest of the string.
 */
fun roomNameToResourceName(roomName: String): String {
    val builder = StringBuilder(ROOM_DRAWABLE_PREFIX.length + roomName.length)
    builder.append(ROOM_DRAWABLE_PREFIX)
    var lastDigit = false
    for (c in roomName) {
        if (c.isLetter()) {
            if (lastDigit) {
                break
            }
            builder.append(c.lowercase(Locale.ROOT))
        } else if (c.isDigit()) {
            builder.append(c)
            lastDigit = true
        }
    }
    return builder.toString()
}

object EmptyImageGetter : ImageGetter {
    override fun getDrawable(source: String?): Drawable {
        return Color.TRANSPARENT.toDrawable().apply {
            setBounds(0, 0, 0, 0)
        }
    }
}

private class ListsTagHandler(res: Resources) : TagHandler {

    private val liStarts = CircularIntArray(4)
    private val leadingMargin: Int
    private val bulletGapWidth: Int

    init {
        val density = res.displayMetrics.density
        leadingMargin = (density * LEADING_MARGIN_DIPS + 0.5f).toInt()
        bulletGapWidth = (density * BULLET_GAP_WIDTH_DIPS + 0.5f).toInt()
    }

    /**
     * @return final output length
     */
    private fun ensureParagraphBoundary(output: Editable): Int {
        var length = output.length
        if (length != 0 && output[length - 1] != '\n') {
            output.insert(length, "\n")
            length++
        }
        return length
    }

    private fun trimStart(output: Editable, start: Int) {
        var end = start
        val length = output.length
        while (end < length && output[end].isWhitespace()) {
            end++
        }
        if (start < end) {
            output.delete(start, end)
        }
    }

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        when (tag) {
            "pre", "PRE" -> ensureParagraphBoundary(output)
            // Unfortunately the following code will be ignored in API 24+ and the native rendering is inferior
            "li", "LI" -> if (opening) {
                liStarts.addLast(ensureParagraphBoundary(output))
            } else if (!liStarts.isEmpty()) {
                val start = liStarts.popLast()
                trimStart(output, start)
                val end = ensureParagraphBoundary(output)
                // Add leading margin to ensure the bullet is not cut off
                output[start, end] = LeadingMarginSpan.Standard(leadingMargin)
                output[start, end] = BulletSpan(bulletGapWidth)
            }
        }
    }

    companion object {
        private const val LEADING_MARGIN_DIPS = 2f
        private const val BULLET_GAP_WIDTH_DIPS = 8f
    }
}