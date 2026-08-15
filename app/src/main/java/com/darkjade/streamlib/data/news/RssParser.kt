package com.darkjade.streamlib.data.news

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class RssItem(
    val title: String,
    val link: String,
    val description: String?,
    val imageUrl: String?,
    val pubDateMs: Long,
)

/**
 * Parses standard RSS 2.0 feeds. Deliberately minimal and defensive — a
 * malformed or unusual feed should never crash the app, just yield whatever
 * items it could parse (or none), matching "if one source fails, others
 * keep working."
 */
object RssParser {

    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    ).map { SimpleDateFormat(it, Locale.US) }

    fun parse(input: InputStream): List<RssItem> {
        return try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)

            val items = mutableListOf<RssItem>()
            var eventType = parser.eventType
            var inItem = false
            var title: String? = null
            var link: String? = null
            var description: String? = null
            var imageUrl: String? = null
            var pubDate: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "item", "entry" -> {
                                inItem = true
                                title = null; link = null; description = null; imageUrl = null; pubDate = null
                            }
                            "title" -> if (inItem) title = safeNextText(parser)
                            "link" -> if (inItem) {
                                // Atom feeds use <link href="..."/>, RSS uses <link>text</link>
                                val href = parser.getAttributeValue(null, "href")
                                link = href ?: safeNextText(parser)
                            }
                            "description", "summary", "content" -> if (inItem) description = safeNextText(parser)
                            "pubdate", "published", "updated" -> if (inItem) pubDate = safeNextText(parser)
                            "enclosure" -> if (inItem) {
                                val type = parser.getAttributeValue(null, "type")
                                if (type == null || type.startsWith("image")) {
                                    imageUrl = parser.getAttributeValue(null, "url") ?: imageUrl
                                }
                            }
                            "thumbnail" -> if (inItem) {
                                imageUrl = parser.getAttributeValue(null, "url") ?: imageUrl
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.lowercase() == "item" || parser.name.lowercase() == "entry") {
                            inItem = false
                            val safeLink = link
                            val safeTitle = title
                            if (safeLink != null && safeTitle != null) {
                                items.add(
                                    RssItem(
                                        title = cleanText(safeTitle),
                                        link = safeLink.trim(),
                                        description = description?.let { cleanText(it) },
                                        imageUrl = imageUrl ?: extractImageFromHtml(description),
                                        pubDateMs = pubDate?.let { parseDate(it) } ?: System.currentTimeMillis(),
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = try { parser.next() } catch (e: Exception) { XmlPullParser.END_DOCUMENT }
            }
            items
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private fun safeNextText(parser: XmlPullParser): String? =
        try { parser.nextText() } catch (e: Exception) { null }

    private fun cleanText(raw: String): String =
        raw.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()

    private fun extractImageFromHtml(html: String?): String? {
        if (html == null) return null
        val match = Regex("""<img[^>]+src=["']([^"']+)["']""").find(html)
        return match?.groupValues?.get(1)
    }

    private fun parseDate(raw: String): Long {
        for (format in dateFormats) {
            try {
                return format.parse(raw)?.time ?: continue
            } catch (e: Exception) {
                // try next format
            }
        }
        return System.currentTimeMillis()
    }
}
