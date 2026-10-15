package org.fnives.tiktokdownloader.data.network.session

interface CookieStore {

    var cookie: String?

    fun clear()
}