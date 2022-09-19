package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.UserPreferencesLocalSource
import org.fnives.tiktokdownloader.data.model.UserPreferences

class SetUserPreferences(private val userPreferencesLocalSource: UserPreferencesLocalSource) {

    suspend operator fun invoke(userPreferences: UserPreferences) {
        userPreferencesLocalSource.set(userPreferences)
    }
}