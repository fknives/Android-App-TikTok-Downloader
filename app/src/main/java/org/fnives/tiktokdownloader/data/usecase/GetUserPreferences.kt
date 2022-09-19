package org.fnives.tiktokdownloader.data.usecase

import org.fnives.tiktokdownloader.data.local.UserPreferencesLocalSource
import org.fnives.tiktokdownloader.data.model.UserPreferences

class GetUserPreferences(private val userPreferencesLocalSource: UserPreferencesLocalSource) {

    operator fun invoke(): UserPreferences = userPreferencesLocalSource.getSync()
}