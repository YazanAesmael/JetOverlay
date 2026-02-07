// jetoverlay/src/main/java/com/yazan/jetoverlay/internal/OverlayViewWrapper.kt

package com.yazan.jetoverlay.internal

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * A custom [FrameLayout] that acts as a "micro-Activity" for floating overlays.
 *
 * Standard Views added via WindowManager do not have a Lifecycle, ViewModelStore,
 * or SavedStateRegistry attached to them by default. This wrapper provides those scopes,
 * enabling full Jetpack Compose support (including `viewModel()` and `rememberSaveable`)
 * within a floating window.
 *
 * @param context The application context.
 */
internal open class OverlayViewWrapper(context: Context) :
    FrameLayout(context),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // --- Lifecycle & State Components ---
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    // The actual Compose host
    private val composeView = ComposeView(context)

    init {
        // 1. Initialize the SavedStateRegistry (must be done before lifecycle)
        savedStateController.performRestore(null)

        // 2. Set the owners on the view root so Compose can find them via Tree composition
        this.setViewTreeLifecycleOwner(this)
        this.setViewTreeViewModelStoreOwner(this)
        this.setViewTreeSavedStateRegistryOwner(this)

        // 3. Start the Lifecycle (Created state)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // 4. Add the ComposeView to this container
        addView(
            composeView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )

        // 5. Define disposal strategy
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    }

    /**
     * Sets the Composable content to be rendered inside this overlay window.
     */
    fun setContent(content: @Composable () -> Unit) {
        composeView.setContent(content)
    }

    // --- Lifecycle Management (Called by OverlayService) ---

    /**
     * Moves the lifecycle to RESUMED, allowing animations and UI updates to run.
     */
    fun resume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * Moves the lifecycle to DESTROYED and clears the ViewModelStore.
     */
    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // Crucial: Clear ViewModels to prevent memory leaks
        store.clear()
    }

    // --- Owner Implementations ---

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry
}