package org.fnives.tiktokdownloader.data.usecase

import kotlinx.coroutines.flow.Flow
import org.fnives.tiktokdownloader.data.local.UserPreferencesLocalSource
import org.fnives.tiktokdownloader.data.model.UserPreferences

class ObserveUserPreferences(private val userPreferencesLocalSource: UserPreferencesLocalSource) {

    operator fun invoke(): Flow<UserPreferences> = userPreferencesLocalSource.get()
}