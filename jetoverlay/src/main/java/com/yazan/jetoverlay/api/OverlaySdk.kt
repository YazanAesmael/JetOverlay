// jetoverlay/src/main/java/com/yazan/jetoverlay/api/OverlaySdk.kt

package com.yazan.jetoverlay.api

import android.content.Context
import android.content.Intent
import com.yazan.jetoverlay.service.OverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The main entry point for the JetOverlay SDK.
 *
 * This singleton object manages the lifecycle, configuration, and state of all floating overlays.
 * It acts as the bridge between your application code and the underlying [OverlayService].
 *
 * **Usage:**
 * 1. Call [initialize] in your `Application.onCreate()`.
 * 2. Use [show] to display an overlay.
 * 3. Use [hide] to remove it.
 */
object OverlaySdk {

    private var contentFactory: OverlayContentFactory? = null

    internal var notificationConfig: OverlayNotificationConfig = OverlayNotificationConfig()

    private val _activeOverlays = MutableStateFlow<Map<String, ActiveOverlay>>(emptyMap())

    /**
     * A [StateFlow] that emits a map of all currently active overlays.
     *
     * The keys are the unique overlay IDs, and the values are the [ActiveOverlay] state objects.
     * You can collect this flow to observe which overlays are currently on screen or to build
     * a control panel UI.
     */
    val activeOverlays: StateFlow<Map<String, ActiveOverlay>> = _activeOverlays.asStateFlow()

    /**
     * Represents the immutable state of a currently active overlay.
     *
     * @property config The configuration used to start this overlay (ID, initial position, etc.).
     * @property payload Optional arbitrary data passed during [show], useful for passing specific state to your Composable factory.
     */
    data class ActiveOverlay(
        val config: OverlayConfig,
        val payload: Any?
    )

    /**
     * Initializes the JetOverlay SDK.
     *
     * This method **must** be called before invoking [show], [hide], or any other SDK methods.
     * It configures the global notification settings and the content factory used to render overlays.
     *
     * @param notificationConfig Configuration for the required foreground service notification.
     * @param factory A functional interface that defines how to render your overlays. It receives a
     * `Modifier` (for dragging), the `id`, and optional `payload`.
     *
     * @throws IllegalStateException if called more than once (though currently safe, it is best practice to call once).
     */
    fun initialize(
        notificationConfig: OverlayNotificationConfig = OverlayNotificationConfig(),
        factory: OverlayContentFactory
    ) {
        this.notificationConfig = notificationConfig
        this.contentFactory = factory
    }

    /**
     * Internal method to retrieve the configured content factory.
     * Used by the [OverlayService] to render content.
     *
     * @throws IllegalStateException if [initialize] has not been called.
     */
    internal fun getContentFactory(): OverlayContentFactory {
        return contentFactory ?: throw IllegalStateException("OverlaySdk.initialize() must be called first. Did you forget to initialize in your Application class?")
    }

    /**
     * Displays a new overlay or updates an existing one.
     *
     * - If the SDK service is not running, this will start the Foreground Service automatically.
     * - If an overlay with the same `config.id` already exists, it will be updated with the new `payload`
     * (triggering a recomposition of your content).
     *
     * @param context Context required to start the Service (e.g., Activity or Application context).
     * @param config Configuration object defining the ID and initial position/size of the overlay.
     * @param payload Optional data to pass to your [OverlayContentFactory]. Defaults to null.
     */
    fun show(context: Context, config: OverlayConfig, payload: Any? = null) {
        _activeOverlays.update { current ->
            current + (config.id to ActiveOverlay(config, payload))
        }
        startService(context)
    }

    /**
     * Removes the overlay with the specified ID from the screen.
     *
     * If this is the last active overlay, the Foreground Service will continue running until stopped explicitly
     * or by the system, but the window will be removed.
     *
     * @param id The unique identifier of the overlay to remove.
     */
    fun hide(id: String) {
        _activeOverlays.update { current ->
            current - id
        }
    }

    /**
     * Checks if an overlay with the given ID is currently active (visible).
     *
     * @param id The unique identifier to check.
     * @return `true` if the overlay is in the active map, `false` otherwise.
     */
    @Suppress("unused") // Public API: Used by library consumers
    fun isOverlayActive(id: String): Boolean = _activeOverlays.value.containsKey(id)

    private fun startService(context: Context) {
        val intent = Intent(context, OverlayService::class.java)
        // Since minSdk is 26, startForegroundService is always available.
        context.startForegroundService(intent)
    }
}