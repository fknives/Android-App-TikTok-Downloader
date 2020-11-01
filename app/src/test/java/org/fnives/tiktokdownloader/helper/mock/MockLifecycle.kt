package org.fnives.tiktokdownloader.helper.mock

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver

class MockLifecycle : Lifecycle() {
    override fun addObserver(observer: LifecycleObserver) {
    }

    override fun removeObserver(observer: LifecycleObserver) {
    }

    override fun getCurrentState(): State = State.CREATED
}