package org.fnives.tiktokdownloader.helper.mock

import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import org.mockito.kotlin.mock

class MockSavedStateRegistryOwner(
    private val lifecycle: Lifecycle = MockLifecycle(),
    private val mockSavedStateRegistry: SavedStateRegistry = mock()
) : SavedStateRegistryOwner {

    override fun getLifecycle(): Lifecycle = lifecycle

    override fun getSavedStateRegistry(): SavedStateRegistry = mockSavedStateRegistry
}