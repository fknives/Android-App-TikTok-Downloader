package org.fnives.tiktokdownloader.helper.mock

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver

class MockLifecycle : Lifecycle() {
    override val currentState: State
        get() = State.CREATED

    override fun addObserver(observer: LifecycleObserver) {
    }

    override fun removeObserver(observer: LifecycleObserver) {
    }
}