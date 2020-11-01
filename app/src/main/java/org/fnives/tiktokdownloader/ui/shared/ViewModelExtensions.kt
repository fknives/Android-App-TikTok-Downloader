package org.fnives.tiktokdownloader.ui.shared

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.properties.ReadOnlyProperty

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