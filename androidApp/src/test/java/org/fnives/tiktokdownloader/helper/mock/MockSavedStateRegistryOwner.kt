package org.fnives.tiktokdownloader.helper.mock

import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import org.mockito.kotlin.mock

class MockSavedStateRegistryOwner(
    override val lifecycle: Lifecycle = MockLifecycle(),
    private val mockSavedStateRegistry: SavedStateRegistry = mock()
) : SavedStateRegistryOwner {

    override val savedStateRegistry: SavedStateRegistry = mockSavedStateRegistry
}