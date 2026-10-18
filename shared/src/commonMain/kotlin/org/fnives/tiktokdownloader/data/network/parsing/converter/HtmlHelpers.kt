package org.fnives.tiktokdownloader.data.network.parsing.converter

/**
 * Returns the text between the end of [prefix] and the next [quote], or `null` if either isn't found.
 * Use when [prefix] already includes the opening quote, e.g. `rel="canonical" href="`.
 */
internal fun String.valueAfter(prefix: String, quote: Char): String? {
    val prefixIndex = indexOf(prefix)
    if (prefixIndex < 0) return null
    val start = prefixIndex + prefix.length
    val end = indexOf(quote, start)
    return if (end < 0) null else substring(start, end)
}

/**
 * Skips to the first [quote] after [prefix], then returns the text until the next [quote].
 * Use when [prefix] does not include the opening quote, e.g. `"playAddr"` followed by `:"…"`.
 */
internal fun String.valueAfterQuoted(prefix: String, quote: Char): String? {
    val prefixIndex = indexOf(prefix)
    if (prefixIndex < 0) return null
    val openingQuote = indexOf(quote, prefixIndex + prefix.length)
    if (openingQuote < 0) return null
    val start = openingQuote + 1
    val end = indexOf(quote, start)
    return if (end < 0) null else substring(start, end)
}
