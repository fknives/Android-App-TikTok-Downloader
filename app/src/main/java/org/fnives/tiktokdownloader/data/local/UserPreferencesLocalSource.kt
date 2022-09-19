package org.fnives.tiktokdownloader.data.local

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import org.fnives.tiktokdownloader.data.local.persistent.UserPreferencesStorage
import org.fnives.tiktokdownloader.data.model.UserPreferences

class UserPreferencesLocalSource(private val userPreferencesStorage: UserPreferencesStorage) {

    private val signal = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST).apply { tryEmit(Unit) }

    fun get(): Flow<UserPreferences> = signal
        .map { getSync() }

    fun getSync() = UserPreferences(alwaysOpenApp = userPreferencesStorage.openAppToIntent)

    suspend fun set(userPreferences: UserPreferences) {
        if (userPreferences.alwaysOpenApp == userPreferencesStorage.openAppToIntent) return
        userPreferencesStorage.openAppToIntent = userPreferences.alwaysOpenApp
        signal.emit(Unit)
    }
}