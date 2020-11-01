package org.fnives.tiktokdownloader.data.local.persistent

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SharedPreferencesManagerImpl private constructor(private val sharedPreferences: SharedPreferences) : SharedPreferencesManager {

    override var captchaTimeoutUntil: Long by LongDelegate(CAPTCHA_TIMEOUT_KEY)
    override var pendingVideos: Set<String> by StringSetDelegate(PENDING_VIDEO_KEY)
    override val pendingVideosFlow by StringSetFlowDelegate(PENDING_VIDEO_KEY)
    override var downloadedVideos: Set<String> by StringSetDelegate(DOWNLOADED_VIDEO_KEY)
    override val downloadedVideosFlow by StringSetFlowDelegate(DOWNLOADED_VIDEO_KEY)

    class LongDelegate(private val key: String) : ReadWriteProperty<SharedPreferencesManagerImpl, Long> {
        override fun setValue(thisRef: SharedPreferencesManagerImpl, property: KProperty<*>, value: Long) {
            thisRef.sharedPreferences.edit().putLong(key, value).apply()
        }

        override fun getValue(thisRef: SharedPreferencesManagerImpl, property: KProperty<*>): Long =
            thisRef.sharedPreferences.getLong(key, 0)
    }

    class StringSetDelegate(private val key: String) : ReadWriteProperty<SharedPreferencesManagerImpl, Set<String>> {
        override fun setValue(thisRef: SharedPreferencesManagerImpl, property: KProperty<*>, value: Set<String>) {
            thisRef.sharedPreferences.edit().putStringSet(key, value).apply()
        }

        override fun getValue(thisRef: SharedPreferencesManagerImpl, property: KProperty<*>): Set<String> =
            thisRef.sharedPreferences.getStringSet(key, emptySet()).orEmpty()
    }

    class StringSetFlowDelegate(private val key: String) : ReadOnlyProperty<SharedPreferencesManagerImpl, Flow<Set<String>>> {
        override fun getValue(thisRef: SharedPreferencesManagerImpl, property: KProperty<*>): Flow<Set<String>> =
            callbackFlow {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    offer(thisRef.getValues())
                }
                thisRef.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                offer(thisRef.getValues())

                awaitClose {
                    thisRef.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

        private fun SharedPreferencesManagerImpl.getValues() : Set<String> =
            sharedPreferences.getStringSet(key, emptySet()).orEmpty()

    }

    companion object {

        private const val SHARED_PREF_NAME = "SHARED_PREF_NAME"
        private const val CAPTCHA_TIMEOUT_KEY = "CAPTCHA_TIMEOUT_KEY"
        private const val PENDING_VIDEO_KEY = "PENDING_VIDEO_KEY"
        private const val DOWNLOADED_VIDEO_KEY = "DOWNLOADED_VIDEO_KEY"

        fun create(context: Context): SharedPreferencesManagerImpl =
            SharedPreferencesManagerImpl(
                context.getSharedPreferences(
                    SHARED_PREF_NAME,
                    Context.MODE_PRIVATE
                )
            )
    }
}