// jetoverlay/src/main/java/com/yazan/jetoverlay/api/OverlayConfig.kt

package com.yazan.jetoverlay.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Configuration for a single overlay instance.
 *
 * This class defines the initial properties of an overlay window, such as its unique identifier
 * and starting position on the screen.
 *
 * @property id A unique identifier for this overlay. Used to update or hide specific overlays.
 * @property initialX The initial X coordinate (in pixels) for the overlay window. Defaults to 0.
 * @property initialY The initial Y coordinate (in pixels) for the overlay window. Defaults to 0.
 * @property width The initial width of the overlay. Defaults to [Dp.Unspecified] (wrap content).
 * @property height The initial height of the overlay. Defaults to [Dp.Unspecified] (wrap content).
 */
data class OverlayConfig(
    val id: String,
    val initialX: Int = 0,
    val initialY: Int = 0,
    val width: Dp = Dp.Unspecified,
    val height: Dp = Dp.Unspecified
)

/**
 * A functional interface for defining the UI content of an overlay.
 *
 * Implementations of this interface are responsible for emitting the Composable content
 * that will be rendered inside the floating window.
 */
fun interface OverlayContentFactory {
    /**
     * Composable content for the overlay.
     *
     * @param modifier A [Modifier] provided by the SDK that **must** be applied to the root
     * layout of your content to enable physics-based dragging and touch handling.
     * @param id The unique identifier of the overlay being rendered.
     * @param payload Optional data passed via [OverlaySdk.show], useful for injecting state.
     */
    @Composable
    fun Content(
        modifier: Modifier,
        id: String,
        payload: Any?,
    )
}