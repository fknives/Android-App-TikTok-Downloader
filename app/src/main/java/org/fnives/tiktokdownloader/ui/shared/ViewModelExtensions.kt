package org.fnives.tiktokdownloader.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <T> CoroutineScope.asLiveData(flow: Flow<T>): LiveData<T> {
    val liveData = MutableLiveData<T>()
    launch {
        flow.collect {
            liveData.postValue(it)
        }
    }

    return liveData
}

fun <T> ViewModel.asLiveData(flow: Flow<T>): LiveData<T> = viewModelScope.asLiveData(flow)