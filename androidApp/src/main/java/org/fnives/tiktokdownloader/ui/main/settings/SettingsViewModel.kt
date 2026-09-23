package org.fnives.tiktokdownloader.ui.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.fnives.tiktokdownloader.data.model.UserPreferences
import org.fnives.tiktokdownloader.data.usecase.ObserveUserPreferences
import org.fnives.tiktokdownloader.data.usecase.SetUserPreferences

class SettingsViewModel(
    getUserPreferences: ObserveUserPreferences,
    private val setUserPreferences: SetUserPreferences
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = getUserPreferences.invoke().stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(alwaysOpenApp = false)
    )

    fun setAlwaysOpenApp(alwaysOpenApp: Boolean) {
        val current = userPreferences.value
        if (current.alwaysOpenApp == alwaysOpenApp) return

        viewModelScope.launch {
            setUserPreferences(current.copy(alwaysOpenApp = alwaysOpenApp))
        }
    }

}