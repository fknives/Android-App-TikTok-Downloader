package org.fnives.tiktokdownloader.worker

/**
 * The global JSON object.
 */
internal external object JSON {
    fun stringify(value: dynamic): String
    fun parse(text: String): dynamic
}
