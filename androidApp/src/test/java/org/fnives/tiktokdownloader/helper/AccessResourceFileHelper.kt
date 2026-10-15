package org.fnives.tiktokdownloader.helper

import java.io.File

fun Any.getResourceFile(path: String): File =
    File(this.javaClass.classLoader!!.getResource(path).file)

fun Any.readResourceFile(path: String): String =
    File(this.javaClass.classLoader!!.getResource(path).file).readText()