package org.fnives.tiktokdownloader.data.local.persistent

private const val SEPARATOR = ";"
private const val TIME_SEPARATOR = '_'

fun String.normalize() = replace(SEPARATOR, "\\$SEPARATOR")
fun String.denormalize() = replace("\\$SEPARATOR", SEPARATOR)

fun List<String>.joinNormalized() : String =
    map { it.normalize() }.joinToString("$SEPARATOR$SEPARATOR")

fun String.separateIntoDenormalized() : List<String> =
    split("$SEPARATOR$SEPARATOR").map { it.denormalize() }

fun String.addTimeAtStart(index: Int = 0) =
    "${System.currentTimeMillis() + index}$TIME_SEPARATOR$this"

fun String.getTimeAndOriginal(): Pair<Long, String> {
    val time = takeWhile { it != TIME_SEPARATOR }.toLong()
    val original = dropWhile { it != TIME_SEPARATOR }.drop(1)
    return time to original
}