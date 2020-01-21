package be.digitalia.fosdem.utils

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

val xmlPullParserFactory by lazy {
    XmlPullParserFactory.newInstance()
}

/*
 * Checks if the current event is the end of the document
 */
inline val XmlPullParser.isEndDocument
    get() = eventType == XmlPullParser.END_DOCUMENT

/*
 * Checks if the current event is a start tag
 */
inline val XmlPullParser.isStartTag
    get() = eventType == XmlPullParser.START_TAG

/*
 * Checks if the current event is a start tag with the specified local name
 */
inline fun XmlPullParser.isStartTag(name: String) = eventType == XmlPullParser.START_TAG && name == this.name

/*
 * Go to the next event and check if the current event is an end tag with the specified local name
 */
inline fun XmlPullParser.isNextEndTag(name: String) = next() == XmlPullParser.END_TAG && name == this.name

/*
 * Skips the start tag and positions the reader on the corresponding end tag
 */
fun XmlPullParser.skipToEndTag() {
    var type = next()
    while (type != XmlPullParser.END_TAG) {
        if (type == XmlPullParser.START_TAG) {
            skipToEndTag()
        }
        type = next()
    }
}