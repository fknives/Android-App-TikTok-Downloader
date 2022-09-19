package org.fnives.tiktokdownloader.ui.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.fnives.tiktokdownloader.data.usecase.ObserveUserPreferences
import org.fnives.tiktokdownloader.data.usecase.SetUserPreferences
import org.fnives.tiktokdownloader.ui.shared.asLiveData

class SettingsViewModel(
    private val getUserPreferences: ObserveUserPreferences,
    private val setUserPreferences: SetUserPreferences
) : ViewModel() {

    val userPreferences = asLiveData(getUserPreferences.invoke())

    fun setAlwaysOpenApp(alwaysOpenApp: Boolean) {
        val userPreferences = userPreferences.value ?: return
        if (userPreferences.alwaysOpenApp == alwaysOpenApp) return

        viewModelScope.launch {
            setUserPreferences(userPreferences.copy(alwaysOpenApp = alwaysOpenApp))
        }
    }

}