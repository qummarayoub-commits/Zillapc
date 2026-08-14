package com.darkjade.streamlib.data.parser

/**
 * Normalizes a title for matching purposes only (never shown to the user).
 * Strips punctuation, collapses whitespace, lowercases — so "Spider-Man",
 * "Spider Man", and "SPIDER MAN!" all group under the same show instead of
 * each episode/file creating its own duplicate entry, which was happening
 * because the previous exact-string match treated any small filename
 * difference as a brand new title.
 */
fun normalizeTitleForMatching(title: String): String {
    return title
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}
