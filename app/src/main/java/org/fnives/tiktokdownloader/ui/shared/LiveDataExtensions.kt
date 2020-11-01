package org.fnives.tiktokdownloader.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <T1, T2, R> combineNullable(
    liveData1: LiveData<T1?>,
    liveData2: LiveData<T2?>,
    mapper: (T1?, T2?) -> R?
): LiveData<R?> {
    val mediatorLiveData = MediatorLiveData<R?>()
    val update = { mediatorLiveData.value = mapper(liveData1.value, liveData2.value) }
    mediatorLiveData.addSource(liveData1) { update() }
    mediatorLiveData.addSource(liveData2) { update() }
    return mediatorLiveData
}