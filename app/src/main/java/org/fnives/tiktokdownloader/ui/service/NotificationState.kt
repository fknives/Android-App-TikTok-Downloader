package org.fnives.tiktokdownloader.ui.service

import androidx.annotation.StringRes

sealed class NotificationState {
    data class Processing(val url: String) : NotificationState()
    data class Error(@StringRes val errorRes: Int) : NotificationState()
    object Finish: NotificationState()
}